import pandas as pd
from fastapi import FastAPI, Request
from pydantic import BaseModel
import pickle
from typing import Optional
import traceback

class FreightInput(BaseModel):
    Peso_total_bruto: float
    Metro_cúbico: float
    Valor_NF: float
    Volume_NF: int
    Tipo_de_frete_NF: str
    Via_de_transporte: str
    UF_emitente_NF: str
    UF_destinatário_NF: str

    class Config:
        alias_generator = lambda string: string.replace('_', ' ')
        populate_by_name = True
        allow_population_by_field_name = True

## Caminhos dos artefatos ##
MODEL_PATH = "api\\api_artifacts\\TMA_Model.pkl"
SCALER_PATH = "api\\api_artifacts\\TMA_Preprocessor.pkl"

## Colunas Categoricas e Numericas ##
NUM_COLS = ["Peso total bruto", "Metro cúbico", "Valor NF", "Volume NF"]
CAT_COLS = ['Tipo de frete NF', 'Via de transporte', "UF emitente NF", "UF destinatário NF"]

## Carregando os artefatos ##
try:
    with open(MODEL_PATH, "rb") as f:
        model = pickle.load(f)

    with open(SCALER_PATH, "rb") as f:
        preprocessor = pickle.load(f)

except FileNotFoundError:
    print(f"Não foi possível carregar os arquivos .pkl.")
    model = None
    scaler = None
    model_columns = []
except Exception as e:
    print(f"{e}")
    model = None
    scaler = None
    model_columns = []


app = FastAPI()

@app.get("/")
async def root():
    return {"Mensagem": "Teste API TMA"}

@app.post("/predict/")
async def predict(data: FreightInput):
    print(data)
    try:
        data_dict_aliased = data.model_dump(by_alias=True)
        input_data = pd.DataFrame([data_dict_aliased])

        # Aplicar o mesmo pré-processamento do treino
        input_data_processed = preprocessor.transform(input_data)

        prediction = model.predict(input_data_processed)

        return {"predicted_transit_time": prediction[0]}
    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)