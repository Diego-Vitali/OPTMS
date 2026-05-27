-- ============================================================================
-- 1. LIMPEZA PREVENTIVA (Garante um ambiente controlado para o teste)
-- ============================================================================
TRUNCATE public.companies, public.lotes_treino, public.dados_treino_operacional, public.catalogo_artefatos_ml RESTART IDENTITY CASCADE;

-- ============================================================================
-- 2. INSERÇÃO DA EMPRESA (Tenant Principal)
-- ============================================================================
INSERT INTO public.companies (name, social_name, document, createdAt, active, apikey)
VALUES ('LUDS Transportes S.A.', 'LUDS LOGISTICA INTERMODAL LTDA', '12345678000199', CURRENT_TIMESTAMP, TRUE, 'APIKEY_LUDS_MLOPS_2026');

-- ============================================================================
-- 3. INSERÇÃO DO LOTE DE TREINO (Gera o input_id = 1)
-- ============================================================================
INSERT INTO public.lotes_treino (company_id, descricao, criado_em)
VALUES (1, 'Massa histórica homologada para validação da esteira MLOps v1', CURRENT_TIMESTAMP);

-- ============================================================================
-- 4. INSERÇÃO DOS DADOS HISTÓRICOS (Target: transit_time_dias)
-- ============================================================================
INSERT INTO public.dados_treino_operacional 
(input_id, peso_total_bruto, metro_cubico, valor_nf, volume_nf, tipo_de_frete_nf, via_de_transporte, uf_emitente_nf, uf_destinatario_nf, transit_time_dias)
VALUES
-- Padrão Rota 1: SP para BA (Longa distância, prazos entre 6 e 8 dias)
(1, 1500.00, 12.50, 45000.00, 30, 'CIF', 'Rodoviário', 'SP', 'BA', 7),
(1, 1400.00, 11.00, 35000.00, 25, 'CIF', 'Rodoviário', 'SP', 'BA', 6),
(1, 1600.00, 14.00, 55000.00, 35, 'FOB', 'Rodoviário', 'SP', 'BA', 8),
(1, 1550.00, 13.00, 42000.00, 28, 'CIF', 'Rodoviário', 'SP', 'BA', 7),
(1, 1480.00, 12.20, 39000.00, 26, 'FOB', 'Rodoviário', 'SP', 'BA', 6),
(1, 1700.00, 15.10, 60000.00, 40, 'CIF', 'Rodoviário', 'SP', 'BA', 8),
(1, 1350.00, 10.50, 32000.00, 22, 'FOB', 'Rodoviário', 'SP', 'BA', 6),
(1, 1520.00, 12.80, 44000.00, 29, 'CIF', 'Rodoviário', 'SP', 'BA', 7),

-- Padrão Rota 2: SP para RJ (Curta distância, rota expressa, prazos entre 1 e 2 dias)
(1, 200.00, 1.50, 8000.00, 5, 'CIF', 'Rodoviário', 'SP', 'RJ', 2),
(1, 250.00, 2.00, 12000.00, 6, 'CIF', 'Rodoviário', 'SP', 'RJ', 1),
(1, 180.00, 1.20, 7500.00, 4, 'FOB', 'Rodoviário', 'SP', 'RJ', 2),
(1, 300.00, 2.50, 15000.00, 8, 'CIF', 'Rodoviário', 'SP', 'RJ', 2),
(1, 220.00, 1.80, 9500.00, 5, 'FOB', 'Rodoviário', 'SP', 'RJ', 1),
(1, 190.00, 1.30, 8200.00, 4, 'CIF', 'Rodoviário', 'SP', 'RJ', 2),
(1, 280.00, 2.20, 14000.00, 7, 'FOB', 'Rodoviário', 'SP', 'RJ', 1),
(1, 210.00, 1.60, 8800.00, 5, 'CIF', 'Rodoviário', 'SP', 'RJ', 2),

-- Padrão Rota 3: SC para BA (Inter-regional complexa, cargas pesadas, prazos entre 9 e 11 dias)
(1, 3500.00, 28.00, 120000.00, 80, 'CIF', 'Rodoviário', 'SC', 'BA', 10),
(1, 3200.00, 25.00, 98000.00, 70, 'CIF', 'Rodoviário', 'SC', 'BA', 9),
(1, 4000.00, 32.00, 150000.00, 95, 'FOB', 'Rodoviário', 'SC', 'BA', 11),
(1, 3600.00, 29.50, 115000.00, 75, 'CIF', 'Rodoviário', 'SC', 'BA', 10),
(1, 3100.00, 24.00, 90000.00, 65, 'FOB', 'Rodoviário', 'SC', 'BA', 9),
(1, 3800.00, 31.00, 140000.00, 90, 'CIF', 'Rodoviário', 'SC', 'BA', 11),
(1, 3300.00, 26.00, 105000.00, 72, 'FOB', 'Rodoviário', 'SC', 'BA', 10),

-- Padrão Rota 4: Cargas Aéreas Urgentes (Qualquer rota com trâmite de 1 dia independente do peso)
(1, 50.00, 0.40, 5000.00, 2, 'CIF', 'Aéreo', 'SP', 'BA', 1),
(1, 70.00, 0.60, 7500.00, 3, 'FOB', 'Aéreo', 'SP', 'BA', 1),
(1, 45.00, 0.35, 4200.00, 1, 'CIF', 'Aéreo', 'SC', 'BA', 1),
(1, 120.00, 1.10, 18000.00, 5, 'CIF', 'Aéreo', 'SP', 'RJ', 1),
(1, 90.00, 0.80, 11000.00, 4, 'FOB', 'Aéreo', 'SP', 'RJ', 1);