import os
import pandas as pd
import numpy as np
import pickle
from fastapi import FastAPI
from typing import Optional, List, Dict, Any
from pydantic import BaseModel, ConfigDict
from logger import get_logger

from sklearn.model_selection import train_test_split, RandomizedSearchCV, KFold, cross_validate
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

# ── Paths ──────────────────────────────────────────────────────────────────
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ARTIFACTS_DIR = os.path.join(BASE_DIR, "api_artifacts")
MODEL_PATH = os.path.join(ARTIFACTS_DIR, "TMA_Model.pkl")
SCALER_PATH = os.path.join(ARTIFACTS_DIR, "TMA_Preprocessor.pkl")

NUM_COLS = ["Peso total bruto", "Metro cúbico", "Valor NF", "Volume NF"]
CAT_COLS = ["Tipo de frete NF", "Via de transporte", "UF emitente NF", "UF destinatário NF"]

logger = get_logger(__name__)

# ── Schemas ────────────────────────────────────────────────────────────────
class FreightInput(BaseModel):
    Peso_total_bruto: float
    Metro_cubico: float
    Valor_NF: float
    Volume_NF: int
    Tipo_de_frete_NF: str
    Via_de_transporte: str
    UF_emitente_NF: str
    UF_destinatario_NF: str

    model_config = ConfigDict(populate_by_name=True) # substituindo class config por ConfigDict porque o suporte pydantic está deprecando a class config


class RetrainRequest(BaseModel):
    records: List[Dict[str, Any]]


# ── Load artifacts ─────────────────────────────────────────────────────────
def load_artifacts():
    try:
        with open(MODEL_PATH, "rb") as f:
            model = pickle.load(f)
        with open(SCALER_PATH, "rb") as f:
            preprocessor = pickle.load(f)
        return model, preprocessor
    except FileNotFoundError:
        print(f"Artefatos não encontrados em {ARTIFACTS_DIR}")
        return None, None
    except Exception as e:
        print(f"Erro ao carregar artefatos: {e}")
        return None, None


# ── Unidades de Machine Learning (Extraídas para Testes) ───────────────────

def validate_and_clean_data(df: pd.DataFrame, required_cols: list) -> pd.DataFrame:
    logger.info(f"Iniciando validação de {len(df)} registros recebidos.")
    missing = [c for c in required_cols if c not in df.columns]
    if missing:
        logger.error(f"Validação falhou. Colunas faltando: {missing}")
        raise ValueError(f"Colunas faltando: {missing}")
    
    df_clean = df[required_cols].dropna()
    linhas_removidas = len(df) - len(df_clean)
    if linhas_removidas > 0:
        logger.warning(f"Foram removidas {linhas_removidas} linhas com valores nulos.")
        
    return df_clean

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

def build_preprocessor(num_cols: list, cat_cols: list) -> ColumnTransformer:
    """Constrói o pipeline de transformação de dados."""
    return ColumnTransformer(
        transformers=[
            ("num", StandardScaler(), num_cols),
            ("cat", OneHotEncoder(handle_unknown="ignore", sparse_output=False), cat_cols),
        ],
        remainder="drop",
    )

def build_base_model(random_state: int = 42):
    """Instancia o modelo base (Futuro ponto de entrada para AutoML)."""
    return RandomForestRegressor(random_state=random_state)

def build_hyperparameter_optimizer(base_model, cv_splits: int = 3, n_iter: int = 10):
    """Configura a estratégia de busca dos melhores hiperparâmetros."""
    param_dist = {
        'n_estimators': [50, 100, 200],
        'max_depth': [None, 10, 20],
        'min_samples_split': [2, 5],
        'min_samples_leaf': [1, 2]
    }
    
    return RandomizedSearchCV(
        estimator=base_model,
        param_distributions=param_dist,
        n_iter=n_iter, 
        cv=cv_splits, 
        scoring='neg_mean_absolute_error',
        random_state=42,
        n_jobs=-1 
    )

def build_cross_validator(n_splits: int = 5):
    """Configura a estratégia de validação cruzada robusta."""
    return KFold(n_splits=n_splits, shuffle=True, random_state=42)

def save_artifacts(model_obj, preprocessor_obj, artifacts_dir: str, 
                   model_filename: str = "TMA_Model.pkl", 
                   scaler_filename: str = "TMA_Preprocessor.pkl"):
    """
    Salva os artefatos de ML no disco. 
    Desacoplado para suportar múltiplos modelos especialistas no futuro.
    """
    os.makedirs(artifacts_dir, exist_ok=True)
    
    m_path = os.path.join(artifacts_dir, model_filename)
    s_path = os.path.join(artifacts_dir, scaler_filename)
    
    with open(m_path, "wb") as f:
        pickle.dump(model_obj, f)
    with open(s_path, "wb") as f:
        pickle.dump(preprocessor_obj, f)
        
    return m_path, s_path

model, preprocessor = load_artifacts()

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
        "modelo_carregado": model is not None,
        "preprocessor_carregado": preprocessor is not None,
    }


@app.post("/predict/")
async def predict(data: FreightInput):
    global model, preprocessor
    if model is None or preprocessor is None:
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
        processed = preprocessor.transform(df)
        prediction = model.predict(processed)
        
        resultado = float(prediction[0])
        logger.info(f"Predição calculada com sucesso: {resultado:.2f} dias.")
        return {"predicted_transit_time": resultado}
        
    except Exception as e:
        logger.exception("Erro interno durante o processamento da predição.")
        return {"error": str(e)}


@app.post("/retrain/")
async def retrain(req: RetrainRequest):
    global model, preprocessor
    if not req.records:
        return {"error": "Nenhum registro enviado para retreino"}

    try:
        df = pd.DataFrame(req.records)

        # Validar colunas necessárias
        required = NUM_COLS + CAT_COLS + ["transit time"]
        df = validate_and_clean_data(df, required)

        if len(df) < 10:
            return {"error": f"Dados insuficientes para treino: {len(df)} registros válidos (mínimo 10)"}

        df = remove_outliers_iqr(df, "transit time")

        # --- MUDANÇA 1: Sem train_test_split ---
        # Usamos 100% dos dados (X e y)
        X = df.drop(columns=["transit time"])
        y = df["transit time"]

        # 1. Pipeline de Dados
        new_preprocessor = build_preprocessor(NUM_COLS, CAT_COLS)
        X_proc = new_preprocessor.fit_transform(X)

        # 2. Instanciação Desacoplada (Pronto para AutoML)
        rf_base = build_base_model()
        optimizer = build_hyperparameter_optimizer(rf_base)
        kf = build_cross_validator(n_splits=5)

        # 3. Executa a Otimização
        optimizer.fit(X_proc, y)
        new_model = optimizer.best_estimator_

        # 4. Avaliação Robusta (K-Fold)
        scoring_metrics = {
            'mae': 'neg_mean_absolute_error',
            'rmse': 'neg_root_mean_squared_error',
            'r2': 'r2'
        }
        cv_results = cross_validate(new_model, X_proc, y, cv=kf, scoring=scoring_metrics)
        
        mae = float(-cv_results['test_mae'].mean())
        rmse = float(-cv_results['test_rmse'].mean())
        r2 = float(cv_results['test_r2'].mean())

        logger.info(f"Treinamento finalizado. Métricas K-Fold - MAE: {mae:.3f} | RMSE: {rmse:.3f} | R2: {r2:.3f}")

        # 5. Treino Final Mestre e Salvamento
        new_model.fit(X_proc, y)

        save_artifacts(new_model, new_preprocessor, ARTIFACTS_DIR)
        logger.info("Novos artefatos salvos em disco com sucesso.")

        global model, preprocessor
        model, preprocessor = new_model, new_preprocessor

        return {
            "status": "ok",
            "n_registros": len(df),
            "mae_kfold": round(mae, 3),
            "rmse_kfold": round(rmse, 3),
            "r2_kfold": round(r2, 3),
        }

    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
