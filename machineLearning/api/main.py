import os
import pandas as pd
import numpy as np
import pickle
from fastapi import FastAPI
from typing import Optional, List, Dict, Any
from pydantic import BaseModel, ConfigDict, Field
from typing import Optional, List, Dict, Any, Literal
from logger import get_logger
import joblib

from sklearn.base import BaseEstimator, TransformerMixin
from sklearn.compose import ColumnTransformer, make_column_selector
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import RandomizedSearchCV

# ── Paths ──────────────────────────────────────────────────────────────────
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ARTIFACTS_DIR = os.path.join(BASE_DIR, "api_artifacts")
MODEL_PATH = os.path.join(ARTIFACTS_DIR, "TMA_Model.pkl")
SCALER_PATH = os.path.join(ARTIFACTS_DIR, "TMA_Preprocessor.pkl")

NUM_COLS = ["Peso total bruto", "Metro cúbico", "Valor NF", "Volume NF", "Densidade", "Valor_por_Kg"]
CAT_COLS = ["Tipo de frete NF", "Via de transporte", "UF emitente NF", "UF destinatário NF", "Rota"]

# --- NOVO: Colunas cruas que a API exige receber do cliente ---
RAW_COLS = [
    "Peso total bruto", "Metro cúbico", "Valor NF", "Volume NF",
    "Tipo de frete NF", "Via de transporte", "UF emitente NF", "UF destinatário NF"
]

logger = get_logger(__name__)

# ── Schemas de Validação de Qualidade (Pydantic V2) ────────────────────────

# 1. Definimos as opções válidas para o Brasil
ESTADOS_BR = Literal[
    "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", 
    "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
]
VIAS_TRANSPORTE = Literal["Rodoviário", "Aéreo", "Marítimo", "Ferroviário", "Cabotagem"]
TIPOS_FRETE = Literal["CIF", "FOB"]

class FreightBase(BaseModel):
    """Regras de negócio que valem tanto para predição quanto para retreino."""
    model_config = ConfigDict(populate_by_name=True)

    Peso_total_bruto: float = Field(..., gt=0, alias="Peso total bruto", description="Peso deve ser estritamente maior que zero")
    Metro_cubico: float = Field(..., gt=0, alias="Metro cúbico", description="Metragem cúbica deve ser maior que zero")
    Valor_NF: float = Field(..., ge=0, alias="Valor NF", description="Valor da NF não pode ser negativo")
    Volume_NF: int = Field(..., gt=0, alias="Volume NF", description="Quantidade de volumes/caixas deve ser no mínimo 1")
    
    Tipo_de_frete_NF: TIPOS_FRETE = Field(..., alias="Tipo de frete NF")
    Via_de_transporte: VIAS_TRANSPORTE = Field(..., alias="Via de transporte")
    UF_emitente_NF: ESTADOS_BR = Field(..., alias="UF emitente NF")
    UF_destinatario_NF: ESTADOS_BR = Field(..., alias="UF destinatário NF")

class FreightInput(FreightBase):
    """Payload para a rota /predict/ (Não tem transit time)"""
    pass

class FreightRetrainRecord(FreightBase):
    """Payload para uma única viagem histórica no /retrain/"""
    transit_time: int = Field(..., gt=0, alias="transit time", description="O tempo de entrega tem que ser de pelo menos 1 dia")

class RetrainRequest(BaseModel):
    """Payload principal da rota /retrain/"""
    # Regra: Garante que o Java nunca mande menos de 10 registros na própria requisição
    records: List[Dict[str, Any]]

# ---

class LogisticsFeatureEngineer(BaseEstimator, TransformerMixin):
    def __init__(self, apply_fe=True):
        # Este é o interruptor que o Grid/Random Search vai ligar e desligar
        self.apply_fe = apply_fe

    def fit(self, X, y=None):
        # Transformers customizados precisam do método fit, mas não aprendemos nada aqui
        return self

    def transform(self, X):
        # Sempre trabalhe com uma cópia para não alterar o DataFrame original
        X_out = X.copy()
        
        # Se o otimizador decidiu desligar o FE, devolvemos os dados crus
        if not self.apply_fe:
            return X_out
            
        # Caso contrário, criamos a mágica
        X_out["Densidade"] = X_out["Peso_total_bruto"] / (X_out["Metro_cubico"] + 0.001)
        X_out["Valor_por_Kg"] = X_out["Valor_NF"] / (X_out["Peso_total_bruto"] + 0.001)
        X_out["Rota"] = X_out["UF_emitente_NF"] + "-" + X_out["UF_destinatario_NF"]
        
        return X_out
    
def build_full_pipeline():
    """
    Cria a esteira completa: Feature Engineering -> Pré-processamento Dinâmico -> Modelo.
    """
    # 1. Pré-processador dinâmico (não usa mais listas fixas!)
    preprocessor = ColumnTransformer(
        transformers=[
            # Exclui explicitamente os dois tipos de texto para pegar só os números
            ("num", StandardScaler(), make_column_selector(dtype_exclude=["object", "string"])),
            
            # Inclui explicitamente os dois tipos de texto
            ("cat", OneHotEncoder(handle_unknown="ignore"), make_column_selector(dtype_include=["object", "string"]))
        ]
    )

    # 2. Monta o Pipeline mestre
    pipeline = Pipeline(steps=[
        ("feature_engineer", LogisticsFeatureEngineer()), # Etapa 1: Cria colunas
        ("preprocessor", preprocessor),                   # Etapa 2: Escala e Encodifica
        ("regressor", RandomForestRegressor(random_state=42, n_jobs=-1)) # Etapa 3: Prediz
    ])
    
    return pipeline

def optimize_and_train(X_train, y_train):
    pipeline = build_full_pipeline()
    
    param_distributions = {
        "feature_engineer__apply_fe": [True, False], 
        "regressor__n_estimators": [100, 200, 300],
        "regressor__max_depth": [10, 20, None],
        "regressor__min_samples_split": [2, 5, 10]
    }
    
    # --- NOVO: Calculando múltiplas métricas ao mesmo tempo ---
    scoring_metrics = {
        'mae': 'neg_mean_absolute_error',
        'rmse': 'neg_root_mean_squared_error',
        'r2': 'r2'
    }
    
    search = RandomizedSearchCV(
        pipeline,
        param_distributions=param_distributions,
        n_iter=10, 
        cv=5,      
        scoring=scoring_metrics,
        refit='mae', # Avisa o GridSearch: "Use o MAE para escolher o grande campeão"
        random_state=42,
        n_jobs=-1  
    )
    
    logger.info("Iniciando busca de hiperparâmetros (Hyperparameter Tuning)...")
    search.fit(X_train, y_train)
    
    logger.info(f"Melhor configuração: {search.best_params_}")
    
    # --- NOVO: Extraindo as métricas do "Grande Campeão" ---
    # O RandomizedSearchCV guarda um histórico de todas as iterações. 
    # Nós pegamos o índice (linha) de qual iteração foi a vencedora:
    best_idx = search.best_index_
    
    # Puxamos as métricas exatas daquela rodada vencedora
    # (Lembrando de multiplicar por -1 os erros, pois o Sklearn os guarda negativos)
    mae = -search.cv_results_['mean_test_mae'][best_idx]
    rmse = -search.cv_results_['mean_test_rmse'][best_idx]
    r2 = search.cv_results_['mean_test_r2'][best_idx]
    
    # Montamos um dicionário elegante com os resultados
    metrics = {
        "mae_kfold": round(float(mae), 3),
        "rmse_kfold": round(float(rmse), 3),
        "r2_kfold": round(float(r2), 3),
        # Bônus legal: Retornar para o Java se a IA decidiu usar suas novas features ou não!
        "usou_feature_engineering": bool(search.best_params_['feature_engineer__apply_fe'])
    }
    
    # Retornamos o melhor modelo E o dicionário de métricas
    return search.best_estimator_, metrics

# ── Load artifacts ─────────────────────────────────────────────────────────
def load_artifacts(dest_dir: str):
    """Carrega o Pipeline Mestre da memória."""
    pipeline_path = os.path.join(dest_dir, "pipeline_completo.pkl")
    if os.path.exists(pipeline_path):
        return joblib.load(pipeline_path)
    return None


# ── Unidades de Machine Learning (Extraídas para Testes) ───────────────────

def validate_and_clean_data(df: pd.DataFrame, required_cols: list) -> None:
    logger.info(f"Iniciando validação de {len(df)} registros recebidos.")
    missing = [c for c in required_cols if c not in df.columns]
    if missing:
        logger.error(f"Validação falhou. Colunas faltando: {missing}")
        raise ValueError(f"Colunas faltando: {missing}")

def remove_outliers_iqr(df: pd.DataFrame, target_col: str) -> pd.DataFrame:
    if df.empty:
        return df
    
    q1, q3 = np.percentile(df[target_col], [25, 75])
    iqr = q3 - q1
    lower_bound = q1 - 1.5 * iqr
    upper_bound = q3 + 1.5 * iqr
    
    df_filtered = df[(df[target_col] >= lower_bound) & (df[target_col] <= upper_bound)]
    outliers_removidos = len(df) - len(df_filtered)
    
    logger.info(f"Remoção de Outliers (IQR): {outliers_removidos} registros anômalos removidos. Limites: [{lower_bound:.2f} a {upper_bound:.2f}]")
    
    return df_filtered

def save_artifacts(pipeline, dest_dir: str):
    """Salva o Pipeline Mestre em disco."""
    os.makedirs(dest_dir, exist_ok=True)
    joblib.dump(pipeline, os.path.join(dest_dir, "pipeline_completo.pkl"))
    logger.info("Pipeline completo salvo no disco.")

model_pipeline = load_artifacts(ARTIFACTS_DIR)

app = FastAPI(
    title="TMS ML API",
    description="API de previsão de transit time e retreino do modelo de Machine Learning",
    version="1.0"
)


# ── Endpoints ──────────────────────────────────────────────────────────────
@app.get("/")
async def root():
    return {"mensagem": "TMS ML API — use /docs para documentação"}


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "modelo_carregado": model_pipeline is not None,
    }


@app.post("/predict/")
async def predict(data: FreightInput):
    global model, preprocessor
    if model_pipeline is None:
        logger.error("Tentativa de predição negada: Modelo não está carregado na memória.")
        return {"error": "Modelo não carregado. Verifique os artefatos em api_artifacts/"}
    
    try:
        logger.info(f"Recebida requisição de predição para rota: {data.UF_emitente_NF} -> {data.UF_destinatario_NF}")
        
        # ... (criação do input_dict e conversão para DataFrame)
        input_dict = {
            "Peso total bruto": data.Peso_total_bruto,
            "Metro cúbico": data.Metro_cubico,
            "Valor NF": data.Valor_NF,
            "Volume NF": data.Volume_NF,
            "Tipo de frete NF": data.Tipo_de_frete_NF,
            "Via de transporte": data.Via_de_transporte,
            "UF emitente NF": data.UF_emitente_NF,
            "UF destinatário NF": data.UF_destinatario_NF,
        }
        df = pd.DataFrame([input_dict])
        prediction = model_pipeline.predict(df) 
        
        resultado = float(prediction[0])
        logger.info(f"Predição calculada com sucesso: {resultado:.2f} dias.")
        return {"predicted_transit_time": resultado}
        
    except Exception as e:
        logger.exception("Erro interno durante o processamento da predição.")
        return {"error": str(e)}


@app.post("/retrain/")
async def retrain(req: RetrainRequest):
    global model_pipeline
    
    valid_records = []
    invalid_count = 0
    
    logger.info(f"Recebidos {len(req.records)} registros. Iniciando limpeza...")

    df_raw = pd.DataFrame(req.records)
    required = RAW_COLS + ["transit time"]

    try:
        validate_and_clean_data(df_raw, required)
    except ValueError as e:
        # Se a função explodir, nós pegamos a mensagem do erro (str(e)) 
        # e devolvemos como um JSON pacífico para o Java e para o Teste
        return {"error": str(e)}

    for r in req.records:
        try:
            # Tentamos forçar a validação individual da linha
            validated_row = FreightRetrainRecord(**r)
            valid_records.append(validated_row.model_dump(by_alias=False))
        except Exception:
            # Se der erro, ignoramos a linha e contamos o erro
            invalid_count += 1

    # Verificamos se, após a limpeza, ainda temos o mínimo necessário
    if len(valid_records) < 10:
        logger.error(f"Treino cancelado. Temos {len(valid_records)} linhas saudáveis (mínimo 10).")
        return {
            "error": "Dados insuficientes após limpeza",
            "linhas_descartadas": invalid_count,
            "linhas_saudaveis": len(valid_records)
        }

    logger.info(f"Limpeza concluída. {len(valid_records)} linhas salvas. {invalid_count} linhas descartadas.")

    try:
        # Agora seguimos o treino normalmente com valid_records...
        df = pd.DataFrame(valid_records)
        
        # Remove outliers lógicos
        df = remove_outliers_iqr(df, "transit_time")

        X = df.drop(columns=["transit_time"])
        y = df["transit_time"]

        best_pipeline, metrics = optimize_and_train(X, y)
        
        # 5. Salva em Produção
        save_artifacts(best_pipeline, ARTIFACTS_DIR)
        model_pipeline = best_pipeline

        return {
        "status": "ok",
        "n_registros_treino": len(X),
        "linhas_descartadas": invalid_count,
        "mae_kfold": metrics["mae_kfold"],
        "rmse_kfold": metrics["rmse_kfold"],
        "r2_kfold": metrics["r2_kfold"],
        "info_modelo": {
            "usou_feature_engineering": metrics["usou_feature_engineering"],
            "mensagem": "Pipeline atualizado e hiperparâmetros otimizados."
        }
    }

    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
