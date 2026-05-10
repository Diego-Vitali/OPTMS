import pytest
import pandas as pd
import numpy as np
import os
import tempfile
from main import (
    validate_and_clean_data, 
    remove_outliers_iqr, 
    build_preprocessor,
    build_base_model,
    build_hyperparameter_optimizer,
    build_cross_validator,
    engineer_features,
    FreightRetrainRecord,
    save_artifacts,                # <-- NOVO
    NUM_COLS, 
    CAT_COLS
)
from pydantic import ValidationError
from sklearn.model_selection import RandomizedSearchCV # <-- NOVO
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import KFold


# ── 1. Testes de Limpeza e Validação de Dados ──────────────────────────────
def test_validate_and_clean_data_sucesso():
    """Garante que a função roda silenciosamente (sem erros) se as colunas estiverem corretas, ignorando colunas extras."""
    
    # DataFrame com as colunas certas e uma coluna inútil
    df_bruto = pd.DataFrame({
        "Peso total bruto": [100.0, 200.0],
        "Metro cúbico": [1.0, 2.0],
        "Coluna_Inutil": ["A", "B"],
        "transit time": [1, 3]
    })
    
    colunas_obrigatorias = ["Peso total bruto", "Metro cúbico", "transit time"]
    
    # Em Pytest, se a função rodar até o final sem levantar exceção, o teste PASSA automaticamente.
    # Não precisamos de 'assert' aqui, o próprio fato de não explodir já é o sucesso!
    validate_and_clean_data(df_bruto, colunas_obrigatorias)

def test_validate_and_clean_data_coluna_faltando():
    """Garante que o sistema grita (levanta exceção) se faltar coluna."""
    df_incompleto = pd.DataFrame({"Peso total bruto": [100.0]})
    colunas_obrigatorias = ["Peso total bruto", "transit time"]
    
    # Verifica se a função gera um ValueError como esperado
    with pytest.raises(ValueError) as erro:
        validate_and_clean_data(df_incompleto, colunas_obrigatorias)
    
    assert "Colunas faltando: ['transit time']" in str(erro.value)


# ── 2. Testes de Remoção de Outliers ───────────────────────────────────────
def test_remove_outliers_iqr():
    """Garante que valores absurdamente altos ou baixos sejam cortados."""
    # 4 viagens normais (2 a 4 dias) e 1 viagem bizarra (50 dias)
    df_com_outlier = pd.DataFrame({
        "transit time": [2, 3, 2, 4, 50],
        "id_viagem": [1, 2, 3, 4, 5]
    })
    
    df_sem_outlier = remove_outliers_iqr(df_com_outlier, "transit time")
    
    # A viagem de 50 dias (outlier) deve ser removida
    assert len(df_sem_outlier) == 4
    assert 50 not in df_sem_outlier["transit time"].values
    assert df_sem_outlier["transit time"].max() <= 4


# ── 3. Testes do Pré-processador (StandardScaler e OneHotEncoder) ──────────
def test_build_preprocessor_transformacao():
    """Garante que o preprocessor lida com as novas listas de colunas."""
    preprocessor = build_preprocessor(["Densidade"], ["Rota"])
    
    df_treino = pd.DataFrame({
        "Densidade": [50.0, 150.0, 30.0],
        "Rota": ["SP-RJ", "MG-BA", "SP-RJ"]
    })
    
    resultado = preprocessor.fit_transform(df_treino)
    assert resultado.dtype == np.float64
    assert resultado.shape[1] == 3

def test_engineer_features():
    """Garante que as novas colunas matemáticas são criadas corretamente."""
    df_raw = pd.DataFrame({
        "Peso total bruto": [100.0],
        "Metro cúbico": [2.0],
        "Valor NF": [500.0],
        "UF emitente NF": ["SP"],
        "UF destinatário NF": ["RJ"]
    })
    
    df_fe = engineer_features(df_raw)
    
    # Verifica a matemática
    assert "Densidade" in df_fe.columns
    assert "Rota" in df_fe.columns
    assert df_fe["Rota"].iloc[0] == "SP-RJ"
    assert round(df_fe["Densidade"].iloc[0], 2) == 49.98 # 100 / (2 + 0.001)

# ── 5. Testes de Instanciação Desacoplada (AutoML Ready) ───────────────────

def test_build_base_model():
    """Garante que a fábrica de modelos retorna o algoritmo esperado."""
    modelo = build_base_model(random_state=99)
    assert isinstance(modelo, RandomForestRegressor)
    assert modelo.random_state == 99

def test_build_hyperparameter_optimizer():
    """Garante que o otimizador está configurado com a grade de parâmetros."""
    modelo_falso = RandomForestRegressor()
    otimizador = build_hyperparameter_optimizer(modelo_falso, cv_splits=4, n_iter=5)
    
    assert isinstance(otimizador, RandomizedSearchCV)
    assert otimizador.cv == 4
    assert otimizador.n_iter == 5
    # Verifica se os hiperparâmetros chave estão no "cardápio" de busca
    assert 'n_estimators' in otimizador.param_distributions
    assert 'max_depth' in otimizador.param_distributions

def test_build_cross_validator():
    """Garante que a estratégia K-Fold seja criada corretamente."""
    kf = build_cross_validator(n_splits=10)
    
    assert isinstance(kf, KFold)
    assert kf.get_n_splits() == 10
    assert kf.shuffle is True # Garantir que os dados sempre sejam embaralhados

# ── 6. Testes de Gerenciamento de Artefatos (I/O) ──────────────────────────

def test_save_artifacts_cria_arquivos():
    """Garante que a função consegue salvar os pickles no diretório dinâmico."""
    # Criamos objetos de mentira (strings em vez de modelos pesados) só para testar a gravação
    modelo_ficticio = "meu_modelo_treinado"
    scaler_ficticio = "meu_scaler_treinado"
    
    # Cria um diretório temporário que se autodestrói após o teste
    with tempfile.TemporaryDirectory() as temp_dir:
        
        # Chama a nossa função passando nomes personalizados (simulando modelos especialistas)
        caminho_m, caminho_s = save_artifacts(
            model_obj=modelo_ficticio, 
            preprocessor_obj=scaler_ficticio, 
            artifacts_dir=temp_dir,
            model_filename="Especialista_SP.pkl",
            scaler_filename="Especialista_SP_Scaler.pkl"
        )
        
        # Verificações
        assert os.path.exists(caminho_m)
        assert os.path.exists(caminho_s)
        assert "Especialista_SP.pkl" in caminho_m


# ── 7. Testes de Qualidade de Dados (Schemas Pydantic) ────────────────────

@pytest.fixture
def base_valida():
    """Retorna um dicionário perfeito para usarmos como base nos testes."""
    return {
        "Peso total bruto": 150.0,
        "Metro cúbico": 1.2,
        "Valor NF": 2500.00,
        "Volume NF": 5,
        "Tipo de frete NF": "CIF",
        "Via de transporte": "Rodoviário",
        "UF emitente NF": "SP",
        "UF destinatário NF": "RJ",
        "transit time": 2
    }

def test_schema_dados_perfeitos(base_valida):
    """Garante que dados corretos são processados e os aliases funcionam."""
    registro = FreightRetrainRecord(**base_valida)
    
    # Verifica se o alias traduziu o espaço para o underline perfeitamente
    assert registro.Peso_total_bruto == 150.0
    assert registro.transit_time == 2

@pytest.mark.parametrize("campo_sujo, valor_sujo, erro_esperado", [
    # --- Regras Matemáticas ---
    ("Peso total bruto", 0, "greater_than"),         # Peso zero
    ("Peso total bruto", -10.5, "greater_than"),     # Peso negativo
    ("Metro cúbico", -1.0, "greater_than"),          # Volume negativo
    ("Valor NF", -0.01, "greater_than_equal"),       # Valor NF negativo (zero é permitido)
    ("Volume NF", 0, "greater_than"),                # Caixas zeradas
    ("transit time", 0, "greater_than"),             # Viagem instantânea/zerada
    
    # --- Regras de Vocabulário (Literal) ---
    ("Tipo de frete NF", "PAGO", "literal_error"),       # Frete inexistente
    ("Tipo de frete NF", "cif", "literal_error"),        # Case sensitive (minúsculo)
    ("Via de transporte", "Bicicleta", "literal_error"), # Via inexistente
    ("UF emitente NF", "XX", "literal_error"),           # Estado falso
    ("UF destinatário NF", "sp", "literal_error"),       # Estado minúsculo
    
    # --- Regras de Tipagem ---
    ("Volume NF", "dez", "int_parsing"),             # String onde deveria ser inteiro
    ("transit time", "dois", "int_parsing")          # String onde deveria ser inteiro
])
def test_schema_barreiras_de_qualidade(base_valida, campo_sujo, valor_sujo, erro_esperado):
    """Garante que o Pydantic explode (ValidationError) ao violar regras de negócio."""
    
    # Sujamos propositalmente um único campo do nosso payload perfeito
    base_valida[campo_sujo] = valor_sujo
    
    # O bloco 'with pytest.raises' garante que o teste SÓ PASSA se ocorrer um erro!
    with pytest.raises(ValidationError) as exc_info:
        FreightRetrainRecord(**base_valida)
    
    # Extraímos a lista de erros do Pydantic
    erros = exc_info.value.errors()
    
    # Verificamos se o erro gerado foi exatamente o tipo de erro matemático/tipagem que queríamos
    assert erros[0]["type"] == erro_esperado