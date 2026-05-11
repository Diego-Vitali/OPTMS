import os
import pytest
from fastapi.testclient import TestClient
from main import app, ARTIFACTS_DIR
import main

client = TestClient(app)

def test_predict_sem_modelo():
    """Garante erro amigável se tentar prever sem o modelo na memória/disco."""
    pipeline_path = os.path.join(ARTIFACTS_DIR, "pipeline_completo.pkl")
    temp_path = os.path.join(ARTIFACTS_DIR, "temp_pipeline.pkl")

    # 1. Esconde o arquivo físico
    if os.path.exists(pipeline_path):
        os.rename(pipeline_path, temp_path)
    
    # 2. Apaga o modelo da memória RAM!
    modelo_em_memoria_backup = main.model_pipeline
    main.model_pipeline = None 

    try:
        response = client.post("/predict/", json={
            "Peso total bruto": 100.0, "Metro cúbico": 1.5, "Valor NF": 1000.0, "Volume NF": 1,
            "Tipo de frete NF": "CIF", "Via de transporte": "Rodoviário",
            "UF emitente NF": "SP", "UF destinatário NF": "RJ"
        })
        assert "error" in response.json()
    finally:
        # Devolve o arquivo e a memória para o lugar (para não quebrar outros testes)
        if os.path.exists(temp_path):
            os.rename(temp_path, pipeline_path)
        main.model_pipeline = modelo_em_memoria_backup


def test_retrain_caminho_feliz():
    """O grande teste: valida o pipeline fim-a-fim."""
    
    pipeline_path = os.path.join(ARTIFACTS_DIR, "pipeline_completo.pkl")
    if os.path.exists(pipeline_path):
        os.remove(pipeline_path)

    # Converti os valores para float onde se espera float (ex: 1.5, 100.0) 
    # para evitar qualquer bloqueio rígido de tipagem do Pydantic
    registros = [
        {
            "Peso total bruto": 100.0 + i, 
            "Metro cúbico": 1.5, 
            "Valor NF": 1000.0, 
            "Volume NF": 1,
            "Tipo de frete NF": "CIF", 
            "Via de transporte": "Rodoviário",
            "UF emitente NF": "SP", 
            "UF destinatário NF": "RJ", 
            "transit time": 2 + (i % 2)
        } for i in range(15)
    ]
    
    response = client.post("/retrain/", json={"records": registros})
    data = response.json()
    
    # --- A MÁGICA ACONTECE AQUI ---
    # Se a API devolver um erro, o teste falha imprimindo EXATAMENTE o que a API reclamou!
    assert "error" not in data, f"A API recusou o pacote de treino! Motivo: {data.get('error')}"
    
    assert response.status_code == 200
    assert data["status"] == "ok"
    assert "mae_kfold" in data
    assert os.path.exists(pipeline_path)

def test_retrain_quebra_contrato():
    """Testa a ETAPA 1: Faltando coluna estrutural no JSON."""
    payload = {"records": [{"Peso total bruto": 100, "transit time": 2}] * 15}
    response = client.post("/retrain/", json=payload)
    
    assert "error" in response.json()
    assert "Colunas faltando" in response.json()["error"]