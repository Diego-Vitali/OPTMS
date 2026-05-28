# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

TMS OPTMS — Transportation Management System for LTL (frete fracionado) carriers. Spring Boot + JSP + MySQL backend with a Python FastAPI ML service for transit time prediction.

## Architecture

Two independently running services:

**1. app/ — Spring Boot 4.0.5, Java 25, WAR packaging**
- REST API (`/api/*`) with Swagger at `/swagger-ui.html`
- JSP web frontend served from embedded Tomcat
- Connects to MySQL database `TMA` on port 3306
- Calls ML API at `http://localhost:8000` via `RestTemplate`

**2. machineLearning/api/ — FastAPI (Python), port 8001**
- `POST /predict/` — transit time prediction
- `POST /retrain/` — retrain model with new order data
- `GET /health` — health check
- Artifacts in `machineLearning/api/api_artifacts/` (pkl files)

## Commands

**Spring Boot (run from `app/`):**
```bash
./mvnw spring-boot:run
./mvnw package -DskipTests
```

**FastAPI ML (run from `machineLearning/api/`):**
```bash
uvicorn main:app --reload --port 8000
```

**Database setup:**
```bash
mysql -u root -p < database/schema.sql
```

## Package Structure (com.optms.app)

- `model/` → JPA entities mapping to `tb_*` tables
- `repository/` → Spring Data JPA interfaces
- `service/` → business logic (injected by @RequiredArgsConstructor)
- `controller/api/` → `@RestController` — documented in Swagger
- `controller/web/` → `@Controller` — renders JSP views
- `util/` → `ExcelFreteParser` (freight table xlsx), `ExcelPedidoParser` (orders xlsx)
- `config/` → `SecurityConfig`, `RestClientConfig`, `OpenApiConfig`

## Key Business Logic

**Freight quotation** (`CotacaoService`): finds the active freight table, locates the matching origin/destination/weight-or-value range via `TabelaFreteFaixaRepository.findFaixa()`, then applies:
- PESO type: `MAX(weight, min_weight) × price_per_kg + GRIS% + AdValorem% + toll`
- VALOR type: `MAX(NF_value × %, min_value) + toll`

**ML prediction** (`PrevisaoService`): sends the physical/fiscal operational fields along with the company_id to the FastAPI /predict/ endpoint. The Python API routes the request to the specific Multi-Tenant artifact (using $O(1)$ Cache-Aside or On-Demand Fallback) and returns the estimated TMA, a dynamic SLA interval (Conformal Prediction), risk alerts (Isolation Forest), and explainability factors (SHAP).

**ML retraining** (`MLService`): creates a new registry in lotes_treino (generating an input_id), inserts the historical freight data into dados_treino_operacional, and POSTs only the lightweight {company_id, input_id} payload to the FastAPI /retrain/ endpoint. The Python API directly queries the PostgreSQL database, executes the AutoML pipeline, and registers the new active .pkl model lineage in catalogo_artefatos_ml.

## Database

Tables: `tb_usuarios`, `tb_tabela_frete`, `tb_tabela_frete_faixa`, `tb_cotacao`, `tb_previsao`, `tb_pedido`, `tb_modelo`, `catalogo_artefatos_ml`, `lotes_treino`, `dados_treino_operacional` 

Default admin: `admin@tms.com` / `admin123` (BCrypt encoded in schema.sql)

## Freight Table xlsx Format

**Sheet "Config"** (key/value pairs): `TIPO` (PESO|VALOR), `TRANSPORTADORA`, `VIGENCIA_INICIO`, `VIGENCIA_FIM`

**Sheet "Tabela"** (row 1 = header): `UF_ORIGEM | UF_DESTINO | FAIXA_INICIAL | FAIXA_FINAL | VALOR_FRETE | PESO_MINIMO | GRIS | AD_VALOREM | PEDAGIO`

## Orders xlsx Format

Single sheet, row 1 = header: `NUMERO_PEDIDO | DATA_EMISSAO | UF_ORIGEM | CEP_ORIGEM | UF_DESTINO | CEP_DESTINO | PESO_BRUTO | METRO_CUBICO | VALOR_NF | QTD_VOLUMES | VIA_TRANSPORTE | TIPO_FRETE | TRANSIT_TIME_REAL`

## Security

- Form login for JSP frontend; HTTP Basic for API clients
- Roles: `ADMIN` (full access) and `USER` (cotação + previsão + histórico)
- CSRF disabled for `/api/**`
- Passwords BCrypt encoded
