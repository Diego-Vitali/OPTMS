import pandas as pd
import numpy as np
import os
import pickle

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

# -----------------------------------------
# CONFIGURAÇÕES
# -----------------------------------------
path_dir = "./"
filename = "Base de dados 2024 - Sudeste.xlsx"
sheetname = "Sheet1"

delete_cols = [
    "CEP destinatário NF", "Cidade emitente NF", "Código IBGE emitente NF", "CEP emitente NF",
    "Cidade destinatário NF", "Código IBGE destinatário NF", "Código tipo de frete NF",
    "Peso aferido"
]
cat_cols = ["Tipo de frete NF", "Via de transporte", "UF emitente NF", "UF destinatário NF"]
num_cols = ["Peso total bruto", "Metro cúbico", "Valor NF", "Volume NF"]
target = "transit time"

artifacts_dir = "../api/api_artifacts"
os.makedirs(artifacts_dir, exist_ok=True)

# -----------------------------------------
# LEITURA
# -----------------------------------------
df = pd.read_excel(os.path.join(path_dir, filename), sheet_name=sheetname)

# Converter números de série do Excel em datas reais
def excel_date(num):
    return pd.to_datetime("1899-12-30") + pd.to_timedelta(num, "D")

df["Data de expedição"] = df["Data de expedição"].apply(excel_date)
df["Data real da entrega"] = df["Data real da entrega"].apply(excel_date)

# Criar target transit time em dias
df[target] = (df["Data real da entrega"] - df["Data de expedição"]).dt.days

# Remover colunas irrelevantes + datas
df = df.drop(columns=delete_cols + ["Data de expedição", "Data real da entrega"], errors="ignore")

# Remover nulos
df = df.dropna()

# -----------------------------------------
# TRATATIVA DE OUTLIERS NO TARGET (IQR)
# -----------------------------------------
q1, q3 = np.percentile(df[target], [25, 75])
iqr = q3 - q1
inf_l = q1 - 1.5 * iqr
sup_l = q3 + 1.5 * iqr
df = df[(df[target] >= inf_l) & (df[target] <= sup_l)]

# -----------------------------------------
# SPLIT TREINO/TESTE
# -----------------------------------------
X = df.drop(columns=[target])
y = df[target]

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# -----------------------------------------
# PREPROCESSAMENTO COM COLUMNTRANSFORMER
# -----------------------------------------
numeric_transformer = StandardScaler()
categorical_transformer = OneHotEncoder(handle_unknown="ignore", sparse_output=False)

preprocessor = ColumnTransformer(
    transformers=[
        ("num", numeric_transformer, num_cols),
        ("cat", categorical_transformer, cat_cols)
    ],
    remainder="drop"
)

X_train_processed = preprocessor.fit_transform(X_train)
X_test_processed = preprocessor.transform(X_test)

# -----------------------------------------
# TREINAMENTO DO MODELO
# -----------------------------------------
model = LinearRegression()
model.fit(X_train_processed, y_train)

# -----------------------------------------
# AVALIAÇÃO
# -----------------------------------------
y_pred = model.predict(X_test_processed)
mae = mean_absolute_error(y_test, y_pred)
mse = mean_squared_error(y_test, y_pred)
rmse = np.sqrt(mse)
r2 = r2_score(y_test, y_pred)

print(f"Média do TMA (dias): {df[target].mean():.2f}")
print(f"MAE: {mae:.2f}")
print(f"MSE: {mse:.2f}")
print(f"RMSE: {rmse:.2f}")
print(f"R²: {r2:.2f}")

# -----------------------------------------
# EXPORTAÇÃO DE ARTEFATOS
# -----------------------------------------
with open(os.path.join(artifacts_dir, "TMA_Model.pkl"), "wb") as f:
    pickle.dump(model, f)

with open(os.path.join(artifacts_dir, "TMA_Preprocessor.pkl"), "wb") as f:
    pickle.dump(preprocessor, f)
