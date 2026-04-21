CREATE TABLE IF NOT EXISTS public.tabela_frete (
    id BIGINT PRIMARY KEY,
    usuario_id BIGINT,
    uf_origem CHAR(2) NOT NULL,
    nome VARCHAR(255),
    ativa BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS public.objeto_frete (
    id BIGINT PRIMARY KEY,
    tabela_id BIGINT,
    tipo_objeto VARCHAR(20),  -- PARTIDA | COMPONENTE
    uf CHAR(2),               -- destino para COMPONENTEs; NULL = aplica a todos
    base_calculo VARCHAR(50),
    tipo_calculo VARCHAR(50),
    config_faixas JSONB,
    nome VARCHAR(100),
    sobre_frete_partida BOOLEAN DEFAULT FALSE
);

-- Índice GIN para buscas em campos JSONB
CREATE INDEX IF NOT EXISTS idx_objeto_frete_faixas ON objeto_frete USING GIN (config_faixas);

-- Garante no máximo uma tabela ativa por UF de origem
CREATE UNIQUE INDEX IF NOT EXISTS uq_tabela_frete_uf_origem_ativa
    ON tabela_frete (uf_origem) WHERE ativa = TRUE;
