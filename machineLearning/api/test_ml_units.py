import pytest
import pandas as pd
import numpy as np
from pydantic import ValidationError
from main import (
    LogisticsFeatureEngineer, 
    validate_and_clean_data, 
    remove_outliers_iqr,
    FreightRetrainRecord
)

# ── 1. Testes do Custom Transformer (Engenharia de Variáveis) ──────────────

def test_feature_engineer_ativo():
    """Garante que as colunas Densidade, Valor_por_Kg e Rota são criadas."""
    df = pd.DataFrame({
        "Peso_total_bruto": [100.0], "Metro_cubico": [2.0], "Valor_NF": [1000.0],
        "UF_emitente_NF": ["SP"], "UF_destinatario_NF": ["RJ"]
    })
    fe = LogisticsFeatureEngineer(apply_fe=True)
    df_trans = fe.transform(df)
    assert "Densidade" in df_trans.columns
    assert df_trans["Rota"].iloc[0] == "SP-RJ"

def test_feature_engineer_inativo():
    """Garante que nada é criado se o otimizador desligar o FE."""
    df = pd.DataFrame({"Peso_total_bruto": [100.0], "Metro_cubico": [2.0]})
    fe = LogisticsFeatureEngineer(apply_fe=False)
    df_trans = fe.transform(df)
    assert "Densidade" not in df_trans.columns

# ── 2. Testes de Limpeza e Outliers ──────────────

def test_validate_and_clean_data_erro():
    """Garante que explode ValueError se faltar coluna no contrato."""
    df = pd.DataFrame({"Peso total bruto": [100.0]})
    with pytest.raises(ValueError, match="Colunas faltando"):
        validate_and_clean_data(df, ["Peso total bruto", "Metro cúbico"])

def test_remove_outliers_iqr():
    """Garante que valores bizarros são removidos."""
    df = pd.DataFrame({"target": [1, 1.1, 1.2, 1, 1.1, 100.0]}) # 100 é outlier
    df_clean = remove_outliers_iqr(df, "target")
    assert len(df_clean) == 5
    assert 100.0 not in df_clean["target"].values

# ── 3. Testes de Qualidade (Pydantic com Parametrize) ──────────────

@pytest.mark.parametrize("campo, valor, erro_tipo", [
    ("Peso total bruto", -1, "greater_than"),
    ("UF emitente NF", "XX", "literal_error"),
    ("Via de transporte", "Foguete", "literal_error"),
    ("transit time", 0, "greater_than")
])
def test_schema_negocio(campo, valor, erro_tipo):
    """Valida todas as nossas travas de segurança de logística."""
    dados = {
        "Peso total bruto": 100, "Metro cúbico": 1, "Valor NF": 100, "Volume NF": 1,
        "Tipo de frete NF": "CIF", "Via de transporte": "Rodoviário",
        "UF emitente NF": "SP", "UF destinatário NF": "RJ", "transit time": 2
    }
    dados[campo] = valor
    with pytest.raises(ValidationError) as exc:
        FreightRetrainRecord(**dados)
    assert exc.value.errors()[0]["type"] == erro_tipo