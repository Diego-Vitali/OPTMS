import os
import pandas as pd
import numpy as np
import pickle
from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional, List, Dict, Any

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

# ── Paths ──────────────────────────────────────────────────────────────────
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ARTIFACTS_DIR = os.path.join(BASE_DIR, "api_artifacts")
MODEL_PATH = os.path.join(ARTIFACTS_DIR, "TMA_Model.pkl")
SCALER_PATH = os.path.join(ARTIFACTS_DIR, "TMA_Preprocessor.pkl")

NUM_COLS = ["Peso total bruto", "Metro cúbico", "Valor NF", "Volume NF"]
CAT_COLS = ["Tipo de frete NF", "Via de transporte", "UF emitente NF", "UF destinatário NF"]


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

    class Config:
        populate_by_name = True


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
        return {"error": "Modelo não carregado. Verifique os artefatos em api_artifacts/"}
    try:
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
        return {"predicted_transit_time": float(prediction[0])}
    except Exception as e:
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
        missing = [c for c in required if c not in df.columns]
        if missing:
            return {"error": f"Colunas faltando: {missing}"}

        df = df[required].dropna()
        if len(df) < 10:
            return {"error": f"Dados insuficientes para treino: {len(df)} registros válidos (mínimo 10)"}

        # Remover outliers no target (IQR)
        q1, q3 = np.percentile(df["transit time"], [25, 75])
        iqr = q3 - q1
        df = df[(df["transit time"] >= q1 - 1.5 * iqr) & (df["transit time"] <= q3 + 1.5 * iqr)]

        X = df.drop(columns=["transit time"])
        y = df["transit time"]
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

        new_preprocessor = ColumnTransformer(
            transformers=[
                ("num", StandardScaler(), NUM_COLS),
                ("cat", OneHotEncoder(handle_unknown="ignore", sparse_output=False), CAT_COLS),
            ],
            remainder="drop",
        )
        X_train_proc = new_preprocessor.fit_transform(X_train)
        X_test_proc = new_preprocessor.transform(X_test)

        new_model = LinearRegression()
        new_model.fit(X_train_proc, y_train)

        y_pred = new_model.predict(X_test_proc)
        mae = float(mean_absolute_error(y_test, y_pred))
        rmse = float(np.sqrt(mean_squared_error(y_test, y_pred)))
        r2 = float(r2_score(y_test, y_pred))

        # Salvar novos artefatos
        os.makedirs(ARTIFACTS_DIR, exist_ok=True)
        with open(MODEL_PATH, "wb") as f:
            pickle.dump(new_model, f)
        with open(SCALER_PATH, "wb") as f:
            pickle.dump(new_preprocessor, f)

        # Recarregar em memória
        model, preprocessor = new_model, new_preprocessor

        return {
            "status": "ok",
            "n_registros": len(df),
            "mae": round(mae, 3),
            "rmse": round(rmse, 3),
            "r2": round(r2, 3),
        }

    except Exception as e:
        return {"error": str(e)}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
