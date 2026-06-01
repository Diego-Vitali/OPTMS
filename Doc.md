# Documentacao completa do OPTMS

## 1. Visao geral

O OPTMS e um sistema de gestao de transporte voltado a transportadoras LTL, isto e, operacoes de frete fracionado. O codigo implementa tres blocos principais:

1. Backend Java Spring Boot, em `app/`, responsavel pela API REST, regras de negocio, persistencia, seguranca por API key e integracao com a API de ML.
2. Servico Python FastAPI, em `machineLearning/api/`, responsavel por previsao de transit time, retreino, registro de modelos e explicabilidade.
3. Frontend PHP server-side, em `frontend-php/`, responsavel pela interface web, sessoes, formularios e consumo da API Java.

O projeto roda de forma integrada com `docker-compose.yml`, que sobe PostgreSQL, API ML, backend Spring e frontend PHP. A decisao efetiva do codigo e usar PostgreSQL, nao MySQL: o `pom.xml`, o `application.properties`, o `docker-compose.yml` e o `schema.sql` usam driver PostgreSQL, dialect PostgreSQL e colunas `JSONB`.

## 2. Arquitetura

### 2.1 Componentes

`postgres`

Banco relacional central. Armazena empresas, usuarios, tabelas de frete, componentes de frete, API keys externas, lotes de treino, dados historicos e catalogo de artefatos ML.

`app`

Backend Spring Boot 4.0.5 com Java 25. Expoe endpoints `/api/**`, executa o motor de cotacao, controla multi-tenant por empresa, recebe planilhas XLSX, persiste dados e chama o servico ML por HTTP.

`ml-api`

Servico FastAPI na porta 8001. Consulta diretamente o PostgreSQL para descobrir dados de treino e modelos ativos, salva artefatos `.pkl` em `machineLearning/api/api_artifacts/` e responde predicoes.

`frontend-php`

Aplicacao PHP 8.3 com Apache na porta 8081. Atua como camada web: login, dashboard, CRUDs, upload de planilhas, cotacao e previsao.

### 2.2 Fluxo de integracao

1. Usuario acessa o frontend PHP.
2. Frontend autentica em `/api/auth/login` e guarda `apiKey`, `companyId`, `userId` e perfil em sessao.
3. Frontend chama endpoints Spring usando `X-API-KEY` e, para CRUD interno de usuarios, `X-USER-ID`.
4. Spring identifica a empresa pelos filtros de API key e injeta `authenticatedCompanyId` no request.
5. Spring executa regra local, consulta PostgreSQL ou chama a FastAPI.
6. FastAPI consulta PostgreSQL quando precisa descobrir o modelo ativo ou dados de treino.
7. FastAPI retorna TMA estimado, SLA, risco e explicabilidade ao Spring, que devolve ao frontend.

### 2.3 Justificativa das decisoes

Spring Boot foi usado porque centraliza APIs REST, validacao, JPA, seguranca e injecao de dependencias em um backend robusto para regras transacionais.

PostgreSQL foi usado porque combina integridade relacional, indices, `JSONB` para regras flexiveis de frete e suporte direto ao pipeline ML via SQLAlchemy.

FastAPI foi separado do backend Java para isolar dependencias de machine learning, evitar misturar stack Python com Spring e permitir evoluir modelos sem recompilar o backend.

PHP server-side foi usado como frontend simples, com baixo custo de execucao, formularios tradicionais e chamadas HTTP diretas para a API Java.

API keys foram escolhidas porque o sistema atende empresas e integracoes externas. A chave da empresa controla o tenant, chaves externas limitam integracoes de cotacao/previsao e a chave master controla administracao global.

O modelo de frete usa `tabela_frete` + `objeto_frete` + `config_faixas JSONB` porque as regras de frete variam por transportadora, rota, peso, valor, componente, percentual, multiplicador e politica de excedente. JSONB evita remodelar o banco para cada variacao de regra.

## 3. Banco de dados

### 3.1 Tabelas principais

`companies`

Representa uma empresa/transportadora. Campos principais: `id`, `name`, `social_name`, `document`, `active`, `apikey`. A `apikey` identifica o tenant nas chamadas da API.

`users`

Usuarios vinculados a uma empresa. Campos principais: `company_id`, `email`, `password`, `active`. A senha e armazenada em BCrypt.

`tabela_frete`

Cabecalho de uma tabela de frete. Guarda empresa, nome, tipo, vigencia, UF de origem legada e flag `ativa`.

`objeto_frete`

Componentes da tabela. Pode ser `PARTIDA`, que calcula o frete base, ou `COMPONENTE`, que adiciona encargos. Guarda origem, destino, tipo de calculo e `config_faixas`.

`external_apikeys`

Chaves de acesso externas vinculadas a uma empresa. Servem para integracoes que precisam apenas calcular cotacao ou previsao.

`lotes_treino`

Registra bases disponiveis e treinamentos. Controla `status`, metricas, arquivo gerado, origem de datasets e erros.

`dados_treino_operacional`

Linhas historicas usadas para treinar modelos ML.

`catalogo_artefatos_ml`

Registro de modelos treinados. Guarda `artifacts_id`, empresa, lote de origem, metricas e modelo ativo. Existe indice unico parcial para garantir apenas um modelo ativo por empresa.

## 4. Backend Java Spring Boot

### 4.1 Configuracao e boot

`AppApplication`

Classe principal. `main` chama `SpringApplication.run`. `@SpringBootApplication` ativa auto-configuracao e scanning. `@EnableAsync` prepara suporte a execucao assinc, embora o retreino use `CompletableFuture.runAsync` diretamente.

`application.properties`

Configura datasource PostgreSQL, dialect Hibernate, `ddl-auto=update`, chave master, URL da API ML e limite de upload de 100 MB.

`pom.xml`

Define Spring Boot 4.0.5, Java 25, JPA, Web MVC, Validation, Security, Springdoc OpenAPI, Apache POI, PostgreSQL, Lombok e H2 para testes.

### 4.2 Modelos de dominio

`Company`

Entidade JPA da tabela `companies`. Representa o tenant. Sua `apikey` e usada pelos filtros para descobrir a empresa autenticada.

`User`

Entidade JPA da tabela `users`. Contem dados de usuario, empresa, senha e status. O campo `password` usa `@JsonIgnore` para nao vazar hash em respostas.

`TabelaFrete`

Entidade JPA da tabela `tabela_frete`. E o cabecalho da tabela de frete. A regra real fica em `ObjetoFrete`; `ufOrigem` e mantido como legado/listagem.

`ObjetoFrete`

Entidade JPA da tabela `objeto_frete`. Representa uma regra calculavel: frete de partida ou componente adicional. Usa `FaixaCalculoConverter` para persistir `FaixaCalculo` em JSONB.

`ExternalApiKey`

Entidade JPA de chaves externas. Permite que integracoes externas calculem cotacoes/previsoes sem usar a API key principal da empresa.

`FaixaCalculo`

Objeto de valor que descreve regras de calculo. Constantes definem unidades (`PESO`, `VLR_NF`, `FRETE_PARTIDA`), metodos (`MULTIPLICADOR`, `PERCENTUAL`, `VALOR_FIXO`) e politica de excedente.

Funcoes importantes:

- `buscarValor(Double entrada)`: implementa a estrutura legada de faixas. Percorre limites e retorna o valor da primeira faixa que contem a entrada; se exceder, usa `valorExcedente` ou o ultimo valor.
- `buscarRegra(Double entrada)`: implementa a nova estrutura. Primeiro aceita regra constante, depois procura regra por faixa, e por fim aplica politica de excedente sobre a ultima faixa.
- `usaNovaEstrutura()`: indica se `regras` esta preenchido e, portanto, se o motor deve usar a nova logica.

`FaixaCalculo.Regra`

Representa uma faixa individual com inicio, fim e valor.

- `isConstante()`: regra sem faixa, aplicavel sempre.
- `contem(Double entrada)`: verifica se a entrada esta dentro da faixa.
- `faixaFinalOrMax()`: ordena regras sem final como maiores.

`FaixaCalculo.RegraAplicavel`

Record que retorna a regra escolhida, a base usada no calculo e se houve excedente.

`FaixaCalculoConverter`

Converte `FaixaCalculo` para JSON string ao salvar no banco e de JSON string para objeto ao carregar. Usa Jackson e ignora propriedades desconhecidas para tolerar evolucao do formato.

### 4.3 DTOs

`AuthLoginRequest`

Entrada de login com `login` e `password`, ambos obrigatorios.

`AuthLoginResponse`

Resposta de login com usuario, empresa, API key e flag `admin`.

`AuthRegisterRequest`

Payload de registro com nome, email e senha. Esta definido, mas o fluxo atual usa criacao por controllers de usuario.

`UserRequest` e `UserUpdateRequest`

Payloads de criacao/atualizacao de usuario. Validam nome, email e tamanho de senha.

`ExternalApiKeyRequest`

Payload para criar chave externa, com `customName` e `companyId` opcional para admin.

`TabelaFreteRequest`

Payload para cadastrar tabela manualmente. Contem cabecalho e lista de `ObjetoFreteDto`.

`TabelaFreteRequest.ObjetoFreteDto`

Representa cada regra de frete recebida na API ou montada pelo parser XLSX. Mapeia tipo de objeto, rota, calculo e faixas.

`TabelaFreteDetalheResponse`

Retorna uma tabela com seus objetos associados.

`TabelaFreteUploadResponse`

Resumo do upload XLSX: tabela criada, nome, origem, quantidade de objetos e mensagem.

`CotacaoRequest`

Entrada de cotacao: origem, destino, peso e valor da NF.

`CotacaoResponse`

Saida de cotacao. Inclui dados de entrada e uma lista de cotacoes por tabela ativa. Os records internos detalham componentes e total.

`MlPredictRequest`

Entrada de previsao. Usa `@JsonProperty` com nomes operacionais acentuados e `@JsonAlias` com nomes camelCase para aceitar payloads do frontend e da API ML.

`MlPredictResponse`

Saida da previsao. Ignora campos desconhecidos para tolerar evolucao da FastAPI. Mapeia `tma_estimado_dias`, `intervalo_sla_dias` e fatores SHAP.

`MlRetrainUploadResponse`

Resposta comum para upload de base e treinamento. Aceita aliases snake_case/camelCase, status, metricas, ids e mensagens.

`MlTrainRequest`

Payload para iniciar treino com uma ou mais bases disponiveis.

### 4.4 Repositories

Todos estendem `JpaRepository`, o que fornece CRUD padrao e query methods.

`CompanyRepository`

Busca empresa por API key ativa, id ativo, nome parcial e verifica duplicidade de API key.

`UserRepository`

Lista usuarios por empresa/nome, busca usuario ativo por empresa, valida emails duplicados e suporta autenticacao.

`TabelaFreteRepository`

Lista tabelas por empresa, status ativo, UF de origem e id. E a base do motor de cotacao e do CRUD de tabelas.

`ObjetoFreteRepository`

Lista e remove objetos por `tabelaId`. Integra tabela de frete aos componentes calculaveis.

`ExternalApiKeyRepository`

Busca chaves externas por empresa, nome, status, id e API key ativa.

### 4.5 Seguranca

`SecurityConfig`

Desabilita CSRF, define sessao stateless e permite todas as rotas em `authorizeHttpRequests`; a protecao real ocorre nos filtros customizados. Registra `MasterApiKey`, `CotacaoApiKeyFilter` e `CompanyApiKeyFilter`. Expoe `PasswordEncoder` BCrypt.

`MasterApiKey`

Filtro para `/api/**`. Se `X-API-KEY` for igual a `master.api.key`, marca `masterAccess=true`. Rotas administrativas, criacao/alteracao/exclusao de empresas e status de empresas exigem chave master.

`CotacaoApiKeyFilter`

Protege `POST /api/cotacoes` e `POST /api/previsao-entrega`. Aceita tanto API key da empresa quanto API key externa ativa. Ao validar, grava `authenticatedCompanyId`.

`CompanyApiKeyFilter`

Protege rotas de empresa que nao sao admin, auth, criacao de empresa ou rotas tratadas pelo filtro de cotacao/previsao. Valida API key principal da empresa e grava `authenticatedCompanyId`.

Decisao importante: a autorizacao nao depende de sessao HTTP no backend. O frontend mantem sessao propria, mas o backend continua stateless e orientado a headers.

### 4.6 Services

`AuthService`

Responsavel pelo login.

- `login(AuthLoginRequest)`: se login/senha forem `admin/admin`, retorna acesso master com a chave master. Caso contrario, busca usuarios ativos por email, valida empresa ativa, impede ambiguidade de multiplos usuarios ativos com o mesmo email e confere BCrypt.

`UserService`

Centraliza CRUD de usuarios.

- `createUser`: valida campos, normaliza email, impede duplicidade global, criptografa senha e salva.
- `listUsers`: lista usuarios da empresa com filtro opcional de nome.
- `listUsersAsAdmin`: permite admin listar todos ou filtrar por empresa/nome.
- `requireActiveUserInCompany`: garante que `X-USER-ID` pertence a empresa autenticada e esta ativo.
- `updateUser` e `updateUserAsAdmin`: atualizam usuario com ou sem troca de empresa.
- `applyUpdate`: valida nome/email, impede email duplicado, atualiza senha quando informada.
- `desativarPorId`, `desativarPorIdECompany`, `ativarPorId`, `ativarPorIdECompany`: alteram status.
- `excluirPorId`, `excluirPorIdECompany`: removem usuarios.

`CompanyService`

Gerencia empresas.

- `criar`: gera API key unica, preenche data/status e salva.
- `atualizar`: valida nome e atualiza dados cadastrais.
- `desativarPorId`: desativa empresa e chaves externas ativas.
- `ativarPorId`: reativa empresa.
- `obterPorIdECompany`: impede que uma empresa consulte outra.
- `excluirPorId`: remove objetos de frete, tabelas, API keys externas, usuarios e a empresa.
- `generateUniqueApiKey`: usa `SecureRandom` e Base64 URL-safe para chave unica.

`ExternalApiKeyService`

Gerencia chaves externas.

- `criar(ExternalApiKey, Long)` e `criar(String, Long)`: geram API key unica e vinculam a empresa.
- `listar` e `listarComoAdmin`: listam por empresa ou global.
- `obterPorIdECompany`: respeita escopo da empresa ou admin.
- `ativar/desativar/excluir`: executam alteracoes por id e, quando necessario, por empresa.

`TabelaFreteService`

Gerencia tabelas e objetos de frete.

- `criar`: salva cabecalho e todos os objetos associados.
- `criarPorXlsx`: usa `FreightTableExcelParser`, cria tabela e retorna resumo.
- `listar` e `listarComoAdmin`: listam tabelas por empresa ou global.
- `obterDetalhes` e `obterDetalhesComoAdmin`: retornam cabecalho + objetos.
- `ativar/desativar`: alteram status por id ou por id+empresa.
- `toEntity`: converte DTO de objeto para entidade JPA.

`CotacaoService`

Motor central de cotacao.

- `calcular`: busca todas as tabelas ativas da empresa, calcula cada tabela aplicavel e retorna todas as cotacoes validas. Se nao houver tabela ou rota, retorna 404.
- `calcularPorTabela`: carrega objetos, normaliza regras agrupadas, encontra `PARTIDA`, calcula frete base, calcula `COMPONENTE`s aplicaveis e soma total.
- `resolverValor`: decide entre estrutura nova e legada. Na legada, escolhe entrada por peso, valor NF ou frete de partida; em percentual, calcula base * percentual e aplica minimo.
- `resolverNovaEstrutura`: escolhe regra, le unidade, aplica metodo multiplicador, percentual ou valor fixo.
- `possuiRegraAplicavel`: filtra objetos da nova estrutura que realmente tem regra para a entrada.
- `entradaPorUnidade`: traduz unidade para peso, valor NF ou frete base.
- `aplicaRota` e `matchesUf`: aplicam origem/destino com suporte a wildcard `*`, vazio e `TODOS`.
- `normalizarObjetos`: agrupa linhas de mesma regra em um objeto unico, preservando legado e juntando regras JSON.
- `maxFaixaFinal` e `copiarObjeto`: apoiam agrupamento sem mutar diretamente entidades carregadas.

`MlPredictionService`

Integra Spring com FastAPI para previsao.

- `predict`: exige `companyId`, monta payload no formato esperado pela FastAPI, chama `/predict/`, trata resposta vazia, erro de modelo inexistente como 404 e falhas HTTP como 502.
- `isMissingActiveModel`: detecta mensagens de modelo ativo ausente.

`MlRetrainService`

Controla upload de bases, treinamento e catalogo visto pelo frontend.

- `retrainFromXlsx`: alias para upload da base.
- `uploadTrainingDataset`: valida empresa, parseia XLSX, cria lote `DISPONIVEL` e salva linhas historicas.
- `trainFromDatasets`: valida bases disponiveis, cria lote de treinamento combinado e dispara chamada assinc para FastAPI.
- `listTrainingJobs`: lista treinamentos com status diferente de `DISPONIVEL`.
- `listTrainingDatasets`: lista bases disponiveis.
- `listTrainingDatasetRows`: retorna ate 500 linhas de uma base.
- `deleteTrainingDataset`: exclui base disponivel, mas bloqueia se estiver vinculada a modelo.
- `listModels`: lista artefatos registrados com metricas e status ativo.
- `activateModel`: garante um unico modelo ativo por empresa.
- `executeTraining`: chama FastAPI `/retrain/` e marca sucesso ou falha.
- `createDataset`: cria lote `DISPONIVEL` e insere linhas em `dados_treino_operacional`.
- `createTrainingBatchFromDatasets`: cria lote `TREINANDO` combinando dados de bases selecionadas.
- `markCompleted` e `markFailed`: atualizam status, metricas, artefato e erro.
- `validateAvailableDatasets`, `ensureDatasetAccess`, `countRowsForDatasets`, `countTrainingRows`: validam escopo e contagens.
- `normalizeInputIds`, `placeholders`, `joinInputIds`: sanitizam listas usadas em SQL parametrizado.
- `numberAsDouble`, `requiredDouble`, `requiredInteger`, `requiredString`: conversoes seguras de dados.

`SchemaMaintenanceService`

Executa manutencoes no startup. Ajusta colunas legadas, cria/atualiza tabela `companies`, adiciona colunas de ML e cria indice unico de modelo ativo quando possivel. Tambem tolera H2 em testes onde alguns comandos PostgreSQL nao existem.

### 4.7 Utilitarios XLSX

`ExcelSupport`

Funcoes estaticas para leitura robusta de planilhas com Apache POI.

- `mapHeaders`: normaliza cabecalhos para indices.
- `requireHeaders`: exige colunas obrigatorias.
- `text`, `decimal`, `integer`: leem celulas com conversao.
- `isBlank`: detecta linha vazia.
- `normalizeKey`: remove acentos, troca caracteres nao alfanumericos por `_` e padroniza maiusculas.

`FreightTableExcelParser`

Parser de tabelas de frete.

- Espera abas `config`, `frete_partida` e `componente`.
- `parse`: valida arquivo, encontra abas, le configuracao, parseia objetos, exige pelo menos uma regra `PARTIDA` e retorna `TabelaFreteRequest`.
- `parseConfig`: le chave/valor da aba de configuracao.
- `parseObjects`: le linhas de partida ou componente, normaliza UF, unidade, metodo, politica de excedente e agrupa regras.
- `buildObject`: cria DTO com `FaixaCalculo`.
- `normalizeUf`: aceita `TODOS`/`ALL` como wildcard.
- `normalizeUnidade`: aceita peso, valor NF e frete de partida, bloqueando frete de partida na aba de partida.
- `normalizeMetodo`: aceita `*`, `%`, multiplicador, percentual e valor fixo.
- `normalizePoliticaExcedente`: aceita total ou apenas excedente.
- `parseDate`: aceita ISO, `dd/MM/yyyy`, `d/M/yyyy`, `MM/dd/yyyy`, `M/d/yyyy`.
- `resolveLegacyOrigin`: preenche origem legada com origem unica ou `MULTI`.

`TrainingExcelParser`

Parser de bases historicas de ML.

- `parse`: le primeira aba, exige cabecalhos operacionais, normaliza campos, descarta linhas invalidas e retorna registros + quantidade descartada.
- `normalizeUf`, `normalizeFreightType`, `normalizeTransportMode`: padronizam dados categoricos.
- `isValidTrainingRecord`: exige numericos e textos essenciais.
- `TrainingParseResult`: record com registros validos e linhas descartadas.

### 4.8 Controllers REST

`AuthController`

`POST /api/auth/login`: delega login ao `AuthService`.

`companyController`

CRUD de empresas em `/api/empresas`.

- `POST /api/empresas`: cria empresa.
- `PUT /api/empresas/{id}`: atualiza empresa, apenas master.
- `GET /api/empresas`: master lista todas; empresa comum ve apenas a propria.
- `GET /api/empresas/{id}`: master consulta qualquer; empresa comum consulta apenas a propria.
- `PATCH /{id}/desativar`, `PATCH /{id}/ativar`, `DELETE /{id}`: apenas master.
- `containsIgnoreCase`: filtro local de nome para empresa comum.

`UserController`

CRUD de usuarios da propria empresa em `/api/usuarios`.

- Usa `X-USER-ID` para validar o ator interno.
- Permite master operar com `companyId`.
- Impede usuario comum de desativar a si mesmo.
- Metodos privados validam empresa autenticada e parseiam `X-USER-ID`.

`AdminUserController`

CRUD administrativo em `/api/admin/usuarios`. Lista, cria, atualiza, ativa, desativa e exclui usuarios entre empresas.

`ExternalApiKeyController`

CRUD de chaves externas da propria empresa em `/api/external-apikeys`, com suporte a master.

`AdminExternalApiKeyController`

CRUD administrativo de chaves externas em `/api/admin/external-apikeys`, incluindo exclusao.

`TabelaFreteController`

CRUD de tabelas de frete da empresa em `/api/tabelas-frete`.

- Lista, detalha, cria via JSON, cria por XLSX, ativa e desativa.
- Para ativar/desativar, diferencia master e empresa comum.

`AdminTabelaFreteController`

Endpoints administrativos em `/api/admin/tabelas-frete`: listar global/por empresa, detalhar, upload XLSX, ativar e desativar.

`CotacaoController`

`POST /api/cotacoes`: calcula cotacao da empresa autenticada por API key principal ou externa.

`AdminCotacaoController`

`POST /api/admin/cotacoes?companyId=...`: permite cotacao administrativa para empresa escolhida.

`MlController`

Endpoints ML da empresa:

- `GET /api/ml/retrain/jobs`
- `GET /api/ml/datasets`
- `GET /api/ml/datasets/{inputId}/records`
- `DELETE /api/ml/datasets/{inputId}`
- `GET /api/ml/models`
- `PATCH /api/ml/models/{modelId}/activate`
- `POST /api/previsao-entrega`
- `POST /api/ml/retrain/upload-xlsx`
- `POST /api/ml/train`

`AdminMlController`

Versao administrativa em `/api/admin/ml`, com `companyId` opcional/obrigatorio conforme operacao.

`ApiExceptionHandler`

Padroniza erros da API em JSON com `status` e `message`. Trata `ResponseStatusException`, erros de validacao, multipart, parametros ausentes, JSON invalido e argumentos invalidos.

## 5. Servico de Machine Learning

### 5.1 Tecnologias

FastAPI, Pydantic v2, Pandas, NumPy, scikit-learn, SHAP, joblib, SQLAlchemy e psycopg2.

### 5.2 Configuracao

`main.py` monta `DATABASE_URL` a partir de variaveis de ambiente, define `ARTIFACTS_DIR`, listas de colunas numericas/categoricas, faixas discretas de peso/valor e instancia `FastAPI`.

`logger.py`

`get_logger(name)` cria logger padronizado em stdout, adequado para Docker.

### 5.3 Schemas Pydantic

`FreightBase`

Contrato comum para predicao e treino. Valida peso > 0, cubagem > 0, valor NF >= 0, volume > 0, UF brasileira, via de transporte e tipo de frete.

`FreightInput`

Payload de `/predict/`. Herda `FreightBase` e adiciona `company_id`.

`FreightRetrainRecord`

Registro historico com `transit time > 0`. Esta classe permanece util para validacao unitaria, embora o endpoint atual de retreino receba `company_id` e `input_id`.

`RetrainRequest`

Payload de `/retrain/`: empresa e lote de dados.

### 5.4 Pipeline ML

`as_numeric_matrix(matrix)`

Converte saidas sparse/densas do pipeline para matriz NumPy float. Necessario para Isolation Forest e SHAP.

`LogisticsFeatureEngineer`

Transformer scikit-learn customizado.

- `fit`: registra colunas originais.
- `transform`: quando ativo, cria `Densidade`, `Valor_por_Kg` e `Rota`.
- `get_feature_names_out`: informa nomes finais para explicabilidade.

`LUDSSplitConformal`

Predicao conformal customizada para intervalo de SLA.

- `calibrate`: calcula residuos absolutos em base de calibracao.
- `predict`: gera previsao media e intervalo inferior/superior usando quantil dos residuos.

`build_full_pipeline`

Monta pipeline: feature engineering, `ColumnTransformer` dinamico para numericos/categoricos, `StandardScaler`, `OneHotEncoder` e `RandomForestRegressor`.

`optimize_and_train`

Fluxo completo de treinamento:

1. Divide treino/calibracao.
2. Executa `RandomizedSearchCV` com metricas MAE, RMSE e R2.
3. Treina Isolation Forest sobre dados transformados.
4. Calibra o modelo conformal.
5. Inicializa SHAP quando disponivel.
6. Pre-calcula cache de predicoes por combinacoes de origem, destino, via, tipo de frete, faixa de peso e faixa de valor.
7. Retorna artefatos e metricas.

### 5.5 Artefatos e cache

`mapear_registro_para_chave`

Discretiza peso e valor em baldes e monta chave string para busca O(1) no cache.

`save_artifacts` e `load_artifacts`

Salvam/carregam joblib. `load_artifacts` ignora artefatos incompativeis com versao do scikit-learn.

`modelos_em_memoria`

Dicionario global que implementa lazy loading e cache em RAM dos modelos por `artifacts_id`.

### 5.6 Limpeza e validacao

`validate_and_clean_data`

Verifica colunas obrigatorias.

`remove_outliers_iqr`

Remove outliers do alvo por regra IQR.

### 5.7 Endpoints FastAPI

`GET /`

Retorna mensagem simples indicando documentacao em `/docs`.

`GET /health`

Testa conexao com PostgreSQL e informa modelos carregados em memoria. Retorna `ok` ou `degradado`.

`POST /predict/`

Fluxo:

1. Consulta `catalogo_artefatos_ml` para achar modelo ativo da empresa.
2. Carrega o `.pkl` em memoria se ainda nao estiver carregado.
3. Tenta responder pelo `prediction_cache`.
4. Em cache miss, monta DataFrame e calcula on-the-fly.
5. Usa Isolation Forest para risco.
6. Usa conformal prediction para intervalo de SLA.
7. Usa SHAP para fatores explicativos quando disponivel.

Resposta inclui `risco`, `tma_estimado_dias`, `intervalo_sla_dias` e `top_fatores_explicacao`.

`POST /retrain/`

Fluxo:

1. Recebe `company_id` e `input_id`.
2. Consulta `dados_treino_operacional` unido a `lotes_treino`.
3. Renomeia colunas para o formato do pipeline.
4. Separa `X` e `y`.
5. Treina pipeline e gera artefato `modelo_cia{company}_{uuid}.pkl`.
6. Salva artefato no disco e em memoria.
7. Desativa modelos anteriores da empresa.
8. Insere novo modelo ativo em `catalogo_artefatos_ml`.
9. Retorna metricas e ids.

## 6. Frontend PHP

### 6.1 Estrutura

`frontend-php/public/index.php`

Roteador principal. Mapeia rotas HTTP, valida autenticacao, chama a API Java e renderiza views.

`frontend-php/src/ApiClient.php`

Cliente HTTP com suporte a GET, POST JSON, multipart, PATCH, PUT e DELETE.

`frontend-php/src/helpers.php`

Funcoes globais de sessao, autenticacao, validacao de UF, escaping, flashes, URLs publicas e headers.

`frontend-php/src/View.php`

Renderizador. Carrega view, captura conteudo e injeta no layout.

`frontend-php/views/**`

Templates de login, dashboard, usuarios, empresas, cotacoes, tabelas de frete, ML, API keys e erros.

### 6.2 ApiClient

`ApiException`

Excecao com status HTTP e payload decodificado.

`ApiClient`

- `getJson`, `postJson`, `patchJson`, `putJson`, `delete`: montam chamadas HTTP.
- `postMultipart`: envia arquivos XLSX usando multipart manual.
- `request`: injeta `X-API-KEY`, headers extras, executa `file_get_contents` com stream context e traduz erros.
- `buildMultipartBody`: monta corpo multipart.
- `extractStatusCode`, `decodeBody`, `extractErrorMessage`: interpretam resposta.

### 6.3 Helpers

Funcoes de rota e resposta: `route_path`, `redirect`, `e`.

Funcoes de dominio visual: `brazilian_ufs`, `normalize_brazilian_uf`, `validate_brazilian_ufs`, `app_name`, `app_slogan`.

Funcoes de sessao: `flash`, `consume_flashes`, `stash_state`, `consume_state`, `login_user_session`, `logout_user_session`.

Funcoes de autenticacao: `is_authenticated`, `is_admin_session`, `require_authentication`, `require_admin`, `current_user_session`, `current_company_api_key`, `current_user_id`, `current_api_headers`.

Funcoes de erro/API: `guard_authenticated_api_exception`, `should_logout_from_api_exception`.

Funcoes de URL: `public_api_base_url`, `swagger_ui_url`, `openapi_json_url`, `request_scheme`.

### 6.4 Rotas e casos de uso no frontend

Login:

- `GET /login`: formulario.
- `POST /login`: chama `/api/auth/login`, guarda sessao e redireciona.
- `/logout`: encerra sessao.

Painel:

- `/dashboard`: area da empresa.
- `/admin`: area master.

Usuarios:

- `/usuarios`: lista/cria usuarios da empresa.
- `/usuarios/editar`: edita usuario.
- `/usuarios/acao`: ativa/desativa.
- `/admin/usuarios`: CRUD administrativo.

Empresas:

- `/admin/empresas`: lista/cria empresas.
- `/admin/empresas/editar`: edita.
- `/admin/empresas/acao`: ativa, desativa ou exclui.

Tabelas de frete:

- `/tabelas-frete/upload`: upload XLSX.
- `/tabelas-frete`: listagem.
- `/tabelas-frete/{id}`: detalhe.
- `/tabelas-frete/acao`: ativa/desativa.
- Versoes admin em `/admin/tabelas-frete`.

Cotar frete:

- `/cotacoes`: formulario e resultado.
- `/admin/cotacoes`: cotacao informando `company_id`.

Previsao:

- `/previsoes`: formulario de ML e resultado de TMA/SLA/risco.

Treinamento ML:

- `/ml/retrain`: workspace da empresa para upload, bases, jobs e modelos.
- `/ml/train`: inicia treino com bases selecionadas.
- `/ml/datasets/delete`: exclui base disponivel.
- `/ml/models/activate`: ativa modelo.
- Versoes admin em `/admin/ml/**`.

API keys externas:

- `/apikeys`: lista/cria chaves externas da empresa.
- `/apikeys/acao`: ativa/desativa.
- `/admin/apikeys`: administra chaves de todas as empresas.

## 7. Casos de uso principais

### 7.1 Autenticacao

1. Usuario informa email/senha no frontend.
2. Frontend chama `/api/auth/login`.
3. Backend valida usuario ativo e empresa ativa.
4. Resposta inclui API key da empresa ou master key.
5. Frontend guarda sessao e usa API key nas chamadas futuras.

Importancia: separa autenticacao da interface da autorizacao stateless da API. A API nao depende de cookies PHP.

### 7.2 Criacao de empresa

1. Admin chama criacao de empresa.
2. `CompanyService.criar` gera API key unica.
3. Empresa passa a ter tenant proprio para usuarios, tabelas, modelos e chaves externas.

Importancia: e a origem do isolamento multi-tenant.

### 7.3 Upload de tabela de frete

1. Usuario envia XLSX.
2. Spring recebe multipart.
3. `FreightTableExcelParser` valida abas e cabecalhos.
4. Parser transforma linhas em `TabelaFreteRequest`.
5. `TabelaFreteService` salva cabecalho e objetos.

Importancia: permite que regras comerciais complexas sejam carregadas por planilha operacional, sem cadastro linha a linha.

### 7.4 Cotacao de frete

1. Cliente chama `/api/cotacoes` com origem, destino, peso e valor NF.
2. Filtro resolve empresa por API key.
3. `CotacaoService` busca tabelas ativas da empresa.
4. Para cada tabela, encontra `PARTIDA` aplicavel.
5. Calcula frete base.
6. Calcula componentes aplicaveis.
7. Retorna detalhamento e total.

Importancia: e o principal caso de uso transacional do sistema, pois converte tabela de regras em preco de frete.

### 7.5 Criacao de API key externa

1. Empresa cria chave externa com nome.
2. Sistema gera chave unica.
3. Integrador usa essa chave apenas para cotacao/previsao.

Importancia: reduz exposicao da API key principal e separa acessos de parceiros/integracoes.

### 7.6 Upload de base de treinamento

1. Usuario envia XLSX historico.
2. `TrainingExcelParser` normaliza e descarta linhas invalidas.
3. `MlRetrainService` cria lote `DISPONIVEL`.
4. Dados sao persistidos em `dados_treino_operacional`.

Importancia: separa carga de dados de treinamento. A base pode ser inspecionada, combinada ou excluida antes de treinar.

### 7.7 Treinamento de modelo

1. Usuario seleciona uma ou mais bases disponiveis.
2. Spring cria novo lote `TREINANDO` com copia dos registros.
3. Spring chama FastAPI `/retrain/` em segundo plano.
4. FastAPI treina, salva `.pkl` e registra modelo ativo.
5. Spring marca lote como `CONCLUIDO` ou `FALHA`.

Importancia: cria linhagem entre base, lote, modelo e metricas.

### 7.8 Previsao de entrega

1. Cliente envia dados operacionais.
2. Spring adiciona `company_id` e chama FastAPI.
3. FastAPI localiza modelo ativo da empresa.
4. FastAPI responde por cache O(1) quando possivel ou calcula sob demanda.
5. Resultado inclui prazo medio, intervalo SLA, risco e fatores explicativos.

Importancia: traz decisao preditiva multi-tenant integrada ao fluxo operacional.

### 7.9 Ativacao de modelo

1. Usuario/admin escolhe modelo treinado.
2. Spring desativa todos os modelos da empresa.
3. Spring ativa o modelo escolhido.

Importancia: garante previsibilidade operacional. Cada empresa tem no maximo um modelo ativo.

## 8. Testes

### 8.1 Java

`AppApplicationTests.contextLoads`

Teste de smoke para verificar se o contexto Spring sobe. Usa H2 em modo PostgreSQL com propriedades de teste.

### 8.2 Python

`test_ml_units.py`

Testa feature engineering, validacao de colunas, remocao de outliers e travas Pydantic.

`test_integration.py`

Testa `/predict/` sem modelo ativo, valida contrato atual de `/retrain/` e marca o caminho feliz de retreino como `skip` porque requer PostgreSQL populado.

## 9. Operacao

### 9.1 Docker

Comando principal:

```bash
docker compose up --build
```

Servicos:

- PostgreSQL: `localhost:5432`
- Backend Spring: `localhost:8080`
- Frontend PHP: `localhost:8081`
- ML API: exposta internamente como `ml-api:8001`

### 9.2 Execucao local

Backend:

```bash
cd app
./mvnw spring-boot:run
```

ML:

```bash
cd machineLearning/api
uvicorn main:app --reload --port 8001
```

### 9.3 Documentacao OpenAPI

Springdoc fica no backend:

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`

FastAPI:

- Swagger UI: `/docs`

## 10. Pontos de atencao tecnica

1. A descricao inicial do projeto menciona MySQL e JSP, mas o codigo atual usa PostgreSQL e frontend PHP. A documentacao acima segue o codigo real.
2. `SecurityConfig` permite todas as rotas no Spring Security e delega bloqueios aos filtros. Isso funciona, mas exige cuidado ao criar novas rotas.
3. O login master `admin/admin` esta hardcoded em `AuthService`; a chave master vem de configuracao.
4. `companyController` usa nome de classe em minusculo, fora da convencao Java.
5. O retreino assinc usa `CompletableFuture.runAsync` sem executor dedicado; em alta carga, seria melhor configurar executor controlado.
6. `SchemaMaintenanceService` executa DDL no startup. Isso ajuda migracao rapida, mas em producao seria mais previsivel usar Flyway ou Liquibase.
7. O frontend PHP monta multipart manualmente; funciona, mas bibliotecas HTTP poderiam reduzir riscos de edge cases.
8. A FastAPI retorna erro como JSON com chave `error` e status HTTP 200 em algumas falhas. O Spring compensa parcialmente, mas codigos HTTP nativos seriam mais claros.
