import os
import pytest
from fastapi.testclient import TestClient
from main import app, ARTIFACTS_FILE, ARTIFACTS_DIR
import main

client = TestClient(app)

def test_predict_sem_modelo():
    """Garante erro amigável se tentar prever sem o modelo na memória/disco."""
    
    pipeline_path = main.ARTIFACTS_FILE # Usa a variável oficial da API
    temp_path = pipeline_path + ".backup" # Cria um nome temporário de verdade
    
    # 1. Salva o modelo da RAM em uma variável local e zera a memória da API
    modelo_em_memoria_backup = main.mlops_system
    main.mlops_system = None 

    # 2. Esconde o arquivo físico do disco (se existir)
    arquivo_escondido = False
    if os.path.exists(pipeline_path):
        os.rename(pipeline_path, temp_path)
        arquivo_escondido = True

    try:
        # 3. Tenta fazer a predição
        payload = {
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
        
        # 4. Verifica se a API barrou corretamente
        assert "error" in data, "A API deveria ter retornado um erro de modelo não carregado!"
        assert "Modelo não carregado" in data["error"]
        
    finally:
        # 5. RESTAURA TUDO (Crucial para não quebrar os próximos testes!)
        if arquivo_escondido:
            os.rename(temp_path, pipeline_path)
        main.mlops_system = modelo_em_memoria_backup

def test_retrain_caminho_feliz():
    """O grande teste: valida o pipeline fim-a-fim."""
    
    if os.path.exists(ARTIFACTS_FILE):
        os.remove(ARTIFACTS_FILE)

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
        } for i in range(50)
    ]
    
    response = client.post("/retrain/", json={"records": registros})
    data = response.json()
    
    # --- A MÁGICA ACONTECE AQUI ---
    # Se a API devolver um erro, o teste falha imprimindo EXATAMENTE o que a API reclamou!
    assert "error" not in data, f"A API recusou o pacote de treino! Motivo: {data.get('error')}"
    
    assert response.status_code == 200
    assert data["status"] == "ok"
    assert "mae_kfold" in data
    assert os.path.exists(ARTIFACTS_FILE)

def test_retrain_quebra_contrato():
    """Testa a ETAPA 1: Faltando coluna estrutural no JSON."""
    payload = {"records": [{"Peso total bruto": 100, "transit time": 2}] * 15}
    response = client.post("/retrain/", json=payload)
    
    assert "error" in response.json()
    assert "Colunas faltando" in response.json()["error"]