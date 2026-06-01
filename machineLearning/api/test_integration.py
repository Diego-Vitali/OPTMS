import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_predict_sem_modelo():
    """Garante erro amigável se tentar prever sem modelo ativo para a Company."""
    payload = {
        "company_id": 999999,
        "Peso total bruto": 150.0,
        "Metro cúbico": 2.0,
        "Valor NF": 5000.0,
        "Volume NF": 10,
        "Tipo de frete NF": "CIF",
        "Via de transporte": "Rodoviário",
        "UF emitente NF": "SP",
        "UF destinatário NF": "RJ"
    }

    response = client.post("/predict/", json=payload)
    data = response.json()

    assert response.status_code == 200
    assert "error" in data

@pytest.mark.skip(reason="Requer PostgreSQL com lotes_treino/dados_treino_operacional populados pelo Spring Boot.")
def test_retrain_caminho_feliz():
    """Valida o contrato atual de retreino, que recebe apenas Company e lote."""
    response = client.post("/retrain/", json={"company_id": 1, "input_id": 1})
    data = response.json()
    
    assert "error" not in data, f"A API recusou o pacote de treino! Motivo: {data.get('error')}"
    assert response.status_code == 200
    assert data["status"] == "sucesso"
    assert "mae_kfold" in data

def test_retrain_quebra_contrato():
    """O retreino não aceita mais dados brutos; exige company_id e input_id."""
    payload = {"records": [{"Peso total bruto": 100, "transit time": 2}] * 15}
    response = client.post("/retrain/", json=payload)
    
    assert response.status_code == 422
