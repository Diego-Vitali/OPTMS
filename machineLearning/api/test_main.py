import pytest
from fastapi.testclient import TestClient
from main import app

# Instancia o cliente de testes (ele simula o servidor rodando localmente)
client = TestClient(app)

# ── 1. Testes de Healthcheck ───────────────────────────────────────────────
def test_healthcheck():
    """Garante que a API está viva e respondendo."""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert "modelo_carregado" in data


# ── 2. Testes da Rota de Predição (/predict/) ──────────────────────────────
def test_predict_sucesso():
    """Garante que o modelo faz predições com dados válidos."""
    payload_valido = {
        "Peso_total_bruto": 1500.0,
        "Metro_cubico": 10.5,
        "Valor_NF": 25000.0,
        "Volume_NF": 20,
        "Tipo_de_frete_NF": "CIF",
        "Via_de_transporte": "Rodoviário",
        "UF_emitente_NF": "SP",
        "UF_destinatario_NF": "RJ"
    }
    
    response = client.post("/predict/", json=payload_valido)
    
    # Verifica se a requisição foi aceita
    assert response.status_code == 200
    data = response.json()
    
    # Verifica se o tempo de trânsito está na resposta e é um número
    assert "predicted_transit_time" in data
    assert isinstance(data["predicted_transit_time"], float)
    # Garante que não devolveu um valor negativo impossível
    assert data["predicted_transit_time"] >= 0

def test_predict_dados_invalidos():
    """Garante que o Pydantic bloqueia dados com tipos errados (ex: texto no lugar de número)."""
    payload_invalido = {
        "Peso_total_bruto": "MIL QUILOS", # Erro proposital
        "Metro_cubico": 10.5,
        "Valor_NF": 25000.0,
        "Volume_NF": 20,
        "Tipo_de_frete_NF": "CIF",
        "Via_de_transporte": "Rodoviário",
        "UF_emitente_NF": "SP",
        "UF_destinatario_NF": "RJ"
    }
    
    response = client.post("/predict/", json=payload_invalido)
    
    # O FastAPI deve barrar com erro 422 (Unprocessable Entity) antes mesmo de chegar no ML
    assert response.status_code == 422


# ── 3. Testes da Rota de Retreinamento (/retrain/) ─────────────────────────
def test_retrain_dados_insuficientes():
    """Garante que o sistema recusa treinar se tiver menos de 10 registros."""
    # Payload com apenas 1 registro
    payload_pequeno = {
        "records": [
            {
                "Peso total bruto": 120.5, "Metro cúbico": 0.8, "Valor NF": 1500.00, 
                "Volume NF": 3, "Tipo de frete NF": "CIF", "Via de transporte": "Rodoviário", 
                "UF emitente NF": "SP", "UF destinatário NF": "RJ", "transit time": 2
            }
        ]
    }
    
    response = client.post("/retrain/", json=payload_pequeno)
    assert response.status_code == 200
    data = response.json()
    
    # Deve retornar a chave "error" avisando da regra de negócio
    assert "error" in data
    assert "Dados insuficientes" in data["error"]

def test_retrain_colunas_faltando():
    """Garante que o sistema recusa treinar se faltar alguma coluna obrigatória."""
    payload_incompleto = {
        "records": [
            {
                "Peso total bruto": 120.5, 
                "UF emitente NF": "SP", 
                "transit time": 2
                # Faltam as outras colunas
            }
        ] * 30 # Multiplica por 15 para passar na regra dos 10 registros
    }
    
    response = client.post("/retrain/", json=payload_incompleto)
    assert response.status_code == 200
    data = response.json()
    
    assert "error" in data
    assert "Colunas faltando" in data["error"]