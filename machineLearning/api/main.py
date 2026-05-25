import os
import warnings
import pandas as pd
import numpy as np
from fastapi import FastAPI
from typing import List, Dict, Any, Literal
from pydantic import BaseModel, ConfigDict, Field
from logger import get_logger
import joblib

from sklearn.base import BaseEstimator, TransformerMixin
from sklearn.compose import ColumnTransformer, make_column_selector
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.ensemble import RandomForestRegressor, IsolationForest
from sklearn.exceptions import InconsistentVersionWarning
from sklearn.model_selection import RandomizedSearchCV, train_test_split

try:
    import shap
    SHAP_IMPORT_ERROR = None
except Exception as exc:
    shap = None
    SHAP_IMPORT_ERROR = exc

# ── Paths ──────────────────────────────────────────────────────────────────
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ARTIFACTS_DIR = os.path.join(BASE_DIR, "api_artifacts")
ARTIFACTS_FILE = os.path.join(ARTIFACTS_DIR, "optms_mlops_artifacts.pkl")

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
        self.apply_fe = apply_fe

    def fit(self, X, y=None):
        # Mudamos o nome da variável para 'original_cols_' para evitar 
        # a validação estrita e bloqueadora do Scikit-Learn
        if hasattr(X, "columns"):
            self.original_cols_ = np.array(X.columns)
        else:
            self.original_cols_ = np.array([])
        return self

    def transform(self, X):
        X_out = X.copy()
        if not self.apply_fe:
            return X_out
            
        X_out["Densidade"] = X_out["Peso_total_bruto"] / (X_out["Metro_cubico"] + 0.001)
        X_out["Valor_por_Kg"] = X_out["Valor_NF"] / (X_out["Peso_total_bruto"] + 0.001)
        X_out["Rota"] = X_out["UF_emitente_NF"] + "-" + X_out["UF_destinatario_NF"]
        return X_out

    def get_feature_names_out(self, input_features=None):
        # Ignoramos o input_features que o sklearn tenta empurrar 
        # e usamos a nossa própria lista salva com segurança
        base = getattr(self, "original_cols_", np.array([]))
        
        if not self.apply_fe:
            return base
            
        novas_features = np.array(["Densidade", "Valor_por_Kg", "Rota"])
        return np.concatenate([base, novas_features])

class LUDSSplitConformal:
    """
    Motor customizado de Predição Conformal (Garantia de SLA).
    Calcula dinamicamente a margem de erro baseada no histórico de resíduos.
    """
    def __init__(self, estimator):
        self.estimator = estimator
        self.residuals = None

    def calibrate(self, X_calib, y_calib):
        # 1. O modelo já está treinado. Fazemos previsões na base de calibração que ele nunca viu.
        preds = self.estimator.predict(X_calib)
        
        # 2. Guardamos o Erro Absoluto de cada previsão (O quanto ele errou em dias)
        self.residuals = np.abs(y_calib - preds)

    def predict(self, X, alpha=0.10):
        # 1. Faz a previsão normal
        preds = self.estimator.predict(X)
        
        # 2. Pega o percentil dos erros histórico baseado no alpha exigido
        # Ex: alpha = 0.10 -> Busca o erro que cobre 90% dos cenários (Percentil 90)
        q = np.quantile(self.residuals, 1 - alpha)
        
        # 3. Cria os intervalos
        limite_inferior = preds - q
        limite_superior = preds + q
        
        return preds, limite_inferior, limite_superior

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

def optimize_and_train(X, y):
    # 1. Divisão para o MAPIE
    X_train, X_calib, y_train, y_calib = train_test_split(X, y, test_size=0.2, random_state=42)
    
    # 2. Otimização do Pipeline Principal (O Sniper vai PRIMEIRO!)
    pipeline = build_full_pipeline()
    
    param_distributions = {
        "feature_engineer__apply_fe": [True, False], 
        "regressor__n_estimators": [100, 200, 300],
        "regressor__max_depth": [10, 20, None],
        "regressor__min_samples_split": [2, 5, 10]
    }
    
    scoring_metrics = {
        'mae': 'neg_mean_absolute_error',
        'rmse': 'neg_root_mean_squared_error',
        'r2': 'r2'
    }
    
    search = RandomizedSearchCV(
        pipeline, param_distributions=param_distributions, n_iter=10, 
        cv=5, scoring=scoring_metrics, refit='mae', random_state=42, n_jobs=-1  
    )
    
    logger.info("Iniciando busca de hiperparâmetros (Hyperparameter Tuning)...")
    search.fit(X_train, y_train)
    best_pipeline = search.best_estimator_
    
    # --- A MÁGICA DA CORREÇÃO ACONTECE AQUI ---
    # Extraímos a inteligência de tradução de texto para número ANTES de instanciar os outros
    transformadores = best_pipeline[:-1] 

    # 3. Treinamento do Cão de Guarda (Isolation Forest) COM DADOS TRANSFORMADOS
    logger.info("Treinando o Cão de Guarda (Isolation Forest)...")
    X_train_mastigado = transformadores.transform(X_train)
    cao_de_guarda = IsolationForest(contamination=0.01, random_state=42, n_jobs=-1)
    cao_de_guarda.fit(X_train_mastigado)
    
# 4. Calibração da Margem de SLA (Nosso motor customizado)
    logger.info("Calibrando intervalos de confiança (Custom Conformal Prediction)...")
    
    # Envelopamos o melhor modelo do AutoML na nossa classe
    conformal_model = LUDSSplitConformal(estimator=best_pipeline)
    
    # Calibramos as margens de erro
    conformal_model.calibrate(X_calib, y_calib)

    # 5. Inicialização do SHAP (Explicabilidade)
    explainer_shap = None
    if shap is None:
        logger.warning(
            "SHAP indisponivel no ambiente. A API seguira sem explicabilidade. Motivo: %s",
            SHAP_IMPORT_ERROR,
        )
    else:
        logger.info("Instanciando Explicabilidade (SHAP)...")
        modelo_arvore = best_pipeline.named_steps["regressor"]
        explainer_shap = shap.TreeExplainer(modelo_arvore)
    transformadores = best_pipeline[:-1] 

    # 6. Agrupando os artefatos (Substitua 'mapie_model' por 'conformal_model')
    artefatos = {
        "cao_de_guarda": cao_de_guarda,
        "conformal_model": conformal_model, 
        "explainer_shap": explainer_shap,
        "transformadores": transformadores
    }

    # (O restante da função de extrair métricas continua igualzinho...)
    best_idx = search.best_index_
    mae = -search.cv_results_['mean_test_mae'][best_idx]
    rmse = -search.cv_results_['mean_test_rmse'][best_idx]
    r2 = search.cv_results_['mean_test_r2'][best_idx]
    
    metrics = {
        "mae_kfold": round(float(mae), 3),
        "rmse_kfold": round(float(rmse), 3),
        "r2_kfold": round(float(r2), 3),
        "usou_feature_engineering": bool(search.best_params_['feature_engineer__apply_fe'])
    }
    
    return artefatos, metrics

# ── Gerenciamento de Artefatos ─────────────────────────────────────────────
def save_artifacts(artefatos: dict, filepath: str):
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    joblib.dump(artefatos, filepath)
    logger.info("Artefatos do MLOps salvos com sucesso.")

def load_artifacts(filepath: str):
    if os.path.exists(filepath):
        try:
            with warnings.catch_warnings():
                warnings.filterwarnings("error", category=InconsistentVersionWarning)
                return joblib.load(filepath)
        except InconsistentVersionWarning:
            logger.warning(
                "Artefatos ignorados por incompatibilidade de versao do scikit-learn. "
                "Execute /retrain/ para gerar um modelo novo."
            )
        except Exception:
            logger.exception(
                "Falha ao carregar artefatos existentes. A API subira sem modelo carregado."
            )
    return None

mlops_system = load_artifacts(ARTIFACTS_FILE)

# ── Funções Auxiliares ─────────────────────────────────────────────────────
def validate_and_clean_data(df: pd.DataFrame, required_cols: list) -> None:
    missing = [c for c in required_cols if c not in df.columns]
    if missing:
        raise ValueError(f"Colunas faltando: {missing}")

def remove_outliers_iqr(df: pd.DataFrame, target_col: str) -> pd.DataFrame:
    if df.empty:
        return df
    q1, q3 = np.percentile(df[target_col], [25, 75])
    iqr = q3 - q1
    lower_bound = q1 - 1.5 * iqr
    upper_bound = q3 + 1.5 * iqr
    
    df_filtered = df[(df[target_col] >= lower_bound) & (df[target_col] <= upper_bound)]
    logger.info(f"Remoção de Outliers (IQR): {len(df) - len(df_filtered)} registros removidos.")
    return df_filtered

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
        "modelo_carregado": mlops_system is not None,
    }


@app.post("/predict/")
async def predict(data: FreightInput):
    global mlops_system
    if mlops_system is None:
        return {"error": "Modelo não carregado. Faça um POST em /retrain/ primeiro."}
    
    try:
        input_dict = data.model_dump(by_alias=False)
        df = pd.DataFrame([input_dict])
        
        # --- APLICA A TRANSFORMAÇÃO PRIMEIRO ---
        X_transformado = mlops_system["transformadores"].transform(df)
        
        # 1. AVALIAÇÃO DE RISCO (Isolation Forest lê os dados transformados)
        status_anomalia = mlops_system["cao_de_guarda"].predict(X_transformado)[0]
        
        # (O resto do código segue igual...)
        if status_anomalia == -1:
            alerta_risco = "ALTO: Frete atípico. Alargando janela de SLA."
            alpha_req = 0.05 # 95% de confiança (Intervalo mais largo)
        else:
            alerta_risco = "BAIXO: Frete padrão."
            alpha_req = 0.10 # 90% de confiança (Intervalo padrão)
            
        # 2. PREVISÃO E SLA (Custom Conformal)
        predicao_media, lim_inf, lim_sup = mlops_system["conformal_model"].predict(df, alpha=alpha_req)
        
        tma_estimado = float(np.round(predicao_media[0], 1))
        
        # Garantimos que o prazo mínimo nunca será negativo ou zero
        tma_min = max(1, int(np.floor(lim_inf[0]))) 
        tma_max = int(np.ceil(lim_sup[0]))

        top_fatores = []
        explainer_shap = mlops_system.get("explainer_shap")

        # Mantem a API operacional mesmo quando SHAP nao estiver disponivel.
        if explainer_shap is not None:
            X_transformado = mlops_system["transformadores"].transform(df)
            shap_vals = explainer_shap.shap_values(X_transformado)

            # Obtém os nomes das colunas após o preprocessor (OneHot gera colunas novas)
            nomes_cols = mlops_system["transformadores"].get_feature_names_out()

            impactos = list(zip(nomes_cols, shap_vals[0]))
            impactos_ordenados = sorted(impactos, key=lambda x: abs(x[1]), reverse=True)

            top_fatores = [
                {"variavel": str(var).replace("cat__", "").replace("num__", ""), "impacto_dias": round(forca, 2)}
                for var, forca in impactos_ordenados[:3]
            ]
        
        return {
            "risco": alerta_risco,
            "tma_estimado_dias": tma_estimado,
            "intervalo_sla_dias": [tma_min, tma_max],
            "top_fatores_explicacao": top_fatores
        }
        
    except Exception as e:
        logger.exception("Erro interno na rota de predição.")
        return {"error": str(e)}


@app.post("/retrain/")
async def retrain(req: RetrainRequest):
    global mlops_system
    valid_records = []
    invalid_count = 0
    
    logger.info(f"Recebidos {len(req.records)} registros para retreino.")
    df_raw = pd.DataFrame(req.records)
    required = RAW_COLS + ["transit time"]

    try:
        validate_and_clean_data(df_raw, required)
    except ValueError as e:
        return {"error": str(e)}

    for r in req.records:
        try:
            validated_row = FreightRetrainRecord(**r)
            valid_records.append(validated_row.model_dump(by_alias=False))
        except Exception:
            invalid_count += 1

    if len(valid_records) < 50: # Aumentado levemente para comportar o split do MAPIE
        return {"error": f"Dados insuficientes. Temos {len(valid_records)} linhas saudáveis (mínimo exigido é 50)."}

    try:
        df = pd.DataFrame(valid_records)
        df = remove_outliers_iqr(df, "transit_time")

        X = df.drop(columns=["transit_time"])
        y = df["transit_time"]

        artefatos_gerados, metrics = optimize_and_train(X, y)
        
        save_artifacts(artefatos_gerados, ARTIFACTS_FILE)
        mlops_system = artefatos_gerados

        return {
            "status": "ok",
            "n_registros_treino": len(X),
            "linhas_descartadas": invalid_count,
            "mae_kfold": metrics["mae_kfold"],
            "rmse_kfold": metrics["rmse_kfold"],
            "r2_kfold": metrics["r2_kfold"],
            "info_modelo": {
                "usou_feature_engineering": metrics["usou_feature_engineering"],
                "mensagem": "Pipeline atualizado. Isolation Forest, MAPIE e SHAP calibrados com sucesso."
            }
        }
    except Exception as e:
        logger.exception("Erro fatal no retreino.")
        return {"error": str(e)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
