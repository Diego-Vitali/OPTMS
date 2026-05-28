# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TMS OPTMS — Transportation Management System for LTL (frete fracionado) carriers. Spring Boot + JSP + MySQL backend with a Python FastAPI ML service for transit time prediction.

## Architecture

Two independently running services:

**1. app/ — Spring Boot 4.0.5, Java 25, WAR packaging**
- REST API (`/api/*`) with Swagger at `/swagger-ui.html`
- JSP web frontend served from embedded Tomcat
- Connects to MySQL database `TMA` on port 3306
- Calls ML API at `http://localhost:8000` via `RestTemplate`

**2. machineLearning/api/ — FastAPI (Python), port 8000**
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

**Freight quotation** (`CotacaoService`): finds active freight tables containing the requested origin UF, selects the PARTIDA object and applicable COMPONENTEs for the destination UF, then applies each constant or range rule with `VALOR_FIXO`, `PERCENTUAL`, or `MULTIPLICADOR`.

**ML prediction** (`PrevisaoService`): sends 8 fields to FastAPI `/predict/`, stores result in `tb_previsao`.

**ML retraining** (`MLService`): exports all `tb_pedido` records as JSON, POSTs to FastAPI `/retrain/`, updates `tb_modelo` version.

## Database

Tables: `tb_usuarios`, `tb_tabela_frete`, `tb_tabela_frete_faixa`, `tb_cotacao`, `tb_previsao`, `tb_pedido`, `tb_modelo`

Default admin: `admin@tms.com` / `admin123` (BCrypt encoded in schema.sql)

## Freight Table xlsx Format

**Sheet "Config"** (row 1 = header): `Nome Referência | Vigência Início | Vigência Fim`

**Sheet "Frete_Partida"** (row 1 = header): `UF Origem | UF Destino | Forma Calculo | Unidade Faixa | Limite Inicial | Limite Final | Unidade variante | Tipo Calculo | Valor do cálculo`

**Sheet "Componentes"** (row 1 = header): `UF Origem | UF Destino | Nome Componente | Forma Calculo | Unidade Faixa | Limite Inicial | Limite Final | Unidade variante | Tipo Calculo | Valor do cálculo`

Each row represents one specific origin/destination route. `FORMA_CALCULO` is `FAIXA` or `CONSTANTE`; `TIPO_CALCULO` is `VALOR_FIXO`, `PERCENTUAL`, or `MULTIPLICADOR`.

## Orders xlsx Format

Single sheet, row 1 = header: `NUMERO_PEDIDO | DATA_EMISSAO | UF_ORIGEM | CEP_ORIGEM | UF_DESTINO | CEP_DESTINO | PESO_BRUTO | METRO_CUBICO | VALOR_NF | QTD_VOLUMES | VIA_TRANSPORTE | TIPO_FRETE | TRANSIT_TIME_REAL`

## Security

- Form login for JSP frontend; HTTP Basic for API clients
- Roles: `ADMIN` (full access) and `USER` (cotação + previsão + histórico)
- CSRF disabled for `/api/**`
- Passwords BCrypt encoded
