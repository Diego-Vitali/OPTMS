package com.optms.app.service;

import com.optms.app.dto.MlRetrainUploadResponse;
import com.optms.app.repository.CompanyRepository;
import com.optms.app.util.TrainingExcelParser;
import com.optms.app.util.TrainingExcelParser.TrainingParseResult;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class MlRetrainService {

    private final TrainingExcelParser trainingExcelParser;
    private final JdbcTemplate jdbcTemplate;
    private final CompanyRepository companyRepository;
    private final RestTemplate restTemplate;
    private final String mlApiBaseUrl;

    public MlRetrainService(
            TrainingExcelParser trainingExcelParser,
            JdbcTemplate jdbcTemplate,
            CompanyRepository companyRepository,
            @Value("${ml.api.base-url}") String mlApiBaseUrl
    ) {
        this.trainingExcelParser = trainingExcelParser;
        this.jdbcTemplate = jdbcTemplate;
        this.companyRepository = companyRepository;
        this.mlApiBaseUrl = mlApiBaseUrl;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public MlRetrainUploadResponse retrainFromXlsx(MultipartFile file, Long companyId) {
        return uploadTrainingDataset(file, companyId);
    }

    @Transactional
    public MlRetrainUploadResponse uploadTrainingDataset(MultipartFile file, Long companyId) {
        if (companyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId é obrigatório para cadastrar a base de treinamento");
        }

        companyRepository.findByIdAndActiveTrue(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company ativa não encontrada"));

        TrainingParseResult parseResult = trainingExcelParser.parse(file);
        List<Map<String, Object>> records = parseResult.records();
        Long inputId = createDataset(companyId, records, parseResult.discardedRows(), file.getOriginalFilename());

        MlRetrainUploadResponse accepted = new MlRetrainUploadResponse();
        accepted.setStatus("DISPONIVEL");
        accepted.setMessage("Base recebida. Selecione esta e outras bases disponíveis para treinar um modelo.");
        accepted.setCompanyId(companyId);
        accepted.setInputId(inputId);
        accepted.setNRegistrosTreino(records.size());
        accepted.setLinhasDescartadas(parseResult.discardedRows());
        return accepted;
    }

    @Transactional
    public MlRetrainUploadResponse trainFromDatasets(Long companyId, List<Long> inputIds) {
        if (companyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId é obrigatório para treinar o modelo");
        }

        companyRepository.findByIdAndActiveTrue(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company ativa não encontrada"));

        List<Long> normalizedInputIds = normalizeInputIds(inputIds);
        if (normalizedInputIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos uma base de dados para treinar o modelo");
        }

        validateAvailableDatasets(companyId, normalizedInputIds);
        Long trainingInputId = createTrainingBatchFromDatasets(companyId, normalizedInputIds);

        CompletableFuture.runAsync(() -> executeTraining(trainingInputId, companyId));

        MlRetrainUploadResponse accepted = new MlRetrainUploadResponse();
        accepted.setStatus("TREINANDO");
        accepted.setMessage("Treinamento iniciado em segundo plano com as bases selecionadas.");
        accepted.setCompanyId(companyId);
        accepted.setInputId(trainingInputId);
        accepted.setNRegistrosTreino(countTrainingRows(trainingInputId));
        accepted.setLinhasDescartadas(0);
        return accepted;
    }

    public List<Map<String, Object>> listTrainingJobs(Long companyId) {
        String sql = """
                SELECT lote.id AS "inputId",
                       lote.company_id AS "companyId",
                       company.name AS "companyName",
                       lote.status AS status,
                       lote.descricao AS descricao,
                       lote.criado_em AS "criadoEm",
                       lote.finalizado_em AS "finalizadoEm",
                       lote.artifacts_id AS "artifactsId",
                       lote.n_registros_treino AS "nRegistrosTreino",
                       lote.linhas_descartadas AS "linhasDescartadas",
                       lote.mae_kfold AS "maeKfold",
                       lote.rmse_kfold AS "rmseKfold",
                       lote.r2_kfold AS "r2Kfold",
                       lote.origem_input_ids AS "origemInputIds",
                       lote.mensagem_erro AS "mensagemErro"
                FROM public.lotes_treino lote
                LEFT JOIN public.companies company ON company.id = lote.company_id
                WHERE lote.status <> 'DISPONIVEL'
                """;

        if (companyId != null) {
            return jdbcTemplate.queryForList(sql + " AND lote.company_id = ? ORDER BY lote.criado_em DESC, lote.id DESC", companyId);
        }

        return jdbcTemplate.queryForList(sql + " ORDER BY lote.criado_em DESC, lote.id DESC");
    }

    public List<Map<String, Object>> listTrainingDatasets(Long companyId) {
        String sql = """
                SELECT lote.id AS "inputId",
                       lote.company_id AS "companyId",
                       company.name AS "companyName",
                       lote.descricao AS descricao,
                       lote.criado_em AS "criadoEm",
                       lote.n_registros_treino AS "nRegistrosTreino",
                       lote.linhas_descartadas AS "linhasDescartadas"
                FROM public.lotes_treino lote
                LEFT JOIN public.companies company ON company.id = lote.company_id
                WHERE lote.status = 'DISPONIVEL'
                """;

        if (companyId != null) {
            return jdbcTemplate.queryForList(sql + " AND lote.company_id = ? ORDER BY lote.criado_em DESC, lote.id DESC", companyId);
        }

        return jdbcTemplate.queryForList(sql + " ORDER BY lote.criado_em DESC, lote.id DESC");
    }

    public List<Map<String, Object>> listTrainingDatasetRows(Long inputId, Long companyId) {
        ensureDatasetAccess(inputId, companyId);

        return jdbcTemplate.queryForList("""
                SELECT dados.id,
                       dados.input_id AS "inputId",
                       dados.peso_total_bruto AS "pesoTotalBruto",
                       dados.metro_cubico AS "metroCubico",
                       dados.valor_nf AS "valorNf",
                       dados.volume_nf AS "volumeNf",
                       dados.tipo_de_frete_nf AS "tipoFreteNf",
                       dados.via_de_transporte AS "viaTransporte",
                       dados.uf_emitente_nf AS "ufEmitenteNf",
                       dados.uf_destinatario_nf AS "ufDestinatarioNf",
                       dados.transit_time_dias AS "transitTimeDias"
                FROM public.dados_treino_operacional dados
                WHERE dados.input_id = ?
                ORDER BY dados.id
                LIMIT 500
                """, inputId);
    }

    @Transactional
    public void deleteTrainingDataset(Long inputId, Long companyId) {
        ensureDatasetAccess(inputId, companyId);

        Integer modelReferences = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.catalogo_artefatos_ml WHERE input_id = ?",
                Integer.class,
                inputId
        );

        if (modelReferences != null && modelReferences > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta base está vinculada a um modelo e não pode ser excluída");
        }

        jdbcTemplate.update("DELETE FROM public.lotes_treino WHERE id = ?", inputId);
    }

    public List<Map<String, Object>> listModels(Long companyId) {
        String sql = """
                SELECT modelo.id,
                       modelo.artifacts_id AS "artifactsId",
                       modelo.company_id AS "companyId",
                       company.name AS "companyName",
                       modelo.input_id AS "inputId",
                       lote.origem_input_ids AS "origemInputIds",
                       modelo.mae_kfold AS "maeKfold",
                       modelo.rmse_kfold AS "rmseKfold",
                       modelo.r2_kfold AS "r2Kfold",
                       modelo.usou_feature_engineering AS "usouFeatureEngineering",
                       modelo.ativo,
                       modelo.treinado_em AS "treinadoEm"
                FROM public.catalogo_artefatos_ml modelo
                LEFT JOIN public.companies company ON company.id = modelo.company_id
                LEFT JOIN public.lotes_treino lote ON lote.id = modelo.input_id
                """;

        if (companyId != null) {
            return jdbcTemplate.queryForList(sql + " WHERE modelo.company_id = ? ORDER BY modelo.treinado_em DESC, modelo.id DESC", companyId);
        }

        return jdbcTemplate.queryForList(sql + " ORDER BY modelo.treinado_em DESC, modelo.id DESC");
    }

    @Transactional
    public void activateModel(Long modelId, Long companyId) {
        if (modelId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "modelId é obrigatório");
        }

        Long effectiveCompanyId = companyId;
        if (effectiveCompanyId == null) {
            List<Long> companies = jdbcTemplate.queryForList(
                    "SELECT company_id FROM public.catalogo_artefatos_ml WHERE id = ?",
                    Long.class,
                    modelId
            );
            if (companies.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo não encontrado");
            }
            effectiveCompanyId = companies.getFirst();
        } else {
            Integer found = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.catalogo_artefatos_ml WHERE id = ? AND company_id = ?",
                    Integer.class,
                    modelId,
                    companyId
            );
            if (found == null || found == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo não encontrado para esta company");
            }
        }

        jdbcTemplate.update("UPDATE public.catalogo_artefatos_ml SET ativo = FALSE WHERE company_id = ?", effectiveCompanyId);
        jdbcTemplate.update("UPDATE public.catalogo_artefatos_ml SET ativo = TRUE WHERE id = ? AND company_id = ?", modelId, effectiveCompanyId);
    }

    private void executeTraining(Long inputId, Long companyId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(
                Map.of("company_id", companyId, "input_id", inputId),
                headers
        );

        try {
            MlRetrainUploadResponse response = restTemplate.postForObject(
                    mlApiBaseUrl + "/retrain/",
                    requestEntity,
                    MlRetrainUploadResponse.class
            );

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "O serviço de ML retornou uma resposta vazia");
            }

            if (response.getError() != null && !response.getError().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, response.getError());
            }

            markCompleted(inputId, response);
        } catch (RuntimeException exception) {
            markFailed(inputId, exception);
        }
    }

    private Long createDataset(Long companyId, List<Map<String, Object>> records, int discardedRows, String originalFilename) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String description = originalFilename != null && !originalFilename.isBlank()
                ? "Base XLSX: " + originalFilename
                : "Base XLSX via Spring Boot";

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO public.lotes_treino
                    (company_id, descricao, status, n_registros_treino, linhas_descartadas)
                    VALUES (?, ?, 'DISPONIVEL', ?, ?)
                    """,
                    new String[]{"id"}
            );
            statement.setLong(1, companyId);
            statement.setString(2, description);
            statement.setInt(3, records.size());
            statement.setInt(4, discardedRows);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível criar o lote de treino");
        }

        Long inputId = key.longValue();
        for (Map<String, Object> record : records) {
            jdbcTemplate.update("""
                    INSERT INTO public.dados_treino_operacional
                    (input_id, peso_total_bruto, metro_cubico, valor_nf, volume_nf,
                     tipo_de_frete_nf, via_de_transporte, uf_emitente_nf, uf_destinatario_nf, transit_time_dias)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    inputId,
                    requiredDouble(record, "Peso total bruto"),
                    requiredDouble(record, "Metro cúbico"),
                    requiredDouble(record, "Valor NF"),
                    requiredInteger(record, "Volume NF"),
                    requiredString(record, "Tipo de frete NF"),
                    requiredString(record, "Via de transporte"),
                    requiredString(record, "UF emitente NF"),
                    requiredString(record, "UF destinatário NF"),
                    requiredInteger(record, "transit time")
            );
        }

        return inputId;
    }

    private Long createTrainingBatchFromDatasets(Long companyId, List<Long> inputIds) {
        int recordCount = countRowsForDatasets(inputIds);
        if (recordCount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "As bases selecionadas não possuem registros válidos para treinamento");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sourceIds = joinInputIds(inputIds);
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO public.lotes_treino
                    (company_id, descricao, status, n_registros_treino, linhas_descartadas, origem_input_ids)
                    VALUES (?, ?, 'TREINANDO', ?, 0, ?)
                    """,
                    new String[]{"id"}
            );
            statement.setLong(1, companyId);
            statement.setString(2, "Treino com bases: " + sourceIds);
            statement.setInt(3, recordCount);
            statement.setString(4, sourceIds);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível criar o lote de treino");
        }

        Long trainingInputId = key.longValue();
        Object[] args = new Object[inputIds.size() + 1];
        args[0] = trainingInputId;
        for (int index = 0; index < inputIds.size(); index++) {
            args[index + 1] = inputIds.get(index);
        }

        jdbcTemplate.update("""
                INSERT INTO public.dados_treino_operacional
                (input_id, peso_total_bruto, metro_cubico, valor_nf, volume_nf,
                 tipo_de_frete_nf, via_de_transporte, uf_emitente_nf, uf_destinatario_nf, transit_time_dias)
                SELECT ?, peso_total_bruto, metro_cubico, valor_nf, volume_nf,
                       tipo_de_frete_nf, via_de_transporte, uf_emitente_nf, uf_destinatario_nf, transit_time_dias
                FROM public.dados_treino_operacional
                WHERE input_id IN (%s)
                """.formatted(placeholders(inputIds.size())), args);

        return trainingInputId;
    }

    private void markCompleted(Long inputId, MlRetrainUploadResponse response) {
        Map<String, Object> metrics = response.getMetrics() != null ? response.getMetrics() : Map.of();
        Double mae = numberAsDouble(metrics.get("mae_kfold"));
        Double rmse = numberAsDouble(metrics.get("rmse_kfold"));
        Double r2 = numberAsDouble(metrics.get("r2_kfold"));

        if (mae == null) {
            mae = response.getMaeKfold();
        }
        if (rmse == null) {
            rmse = response.getRmseKfold();
        }
        if (r2 == null) {
            r2 = response.getR2Kfold();
        }

        jdbcTemplate.update("""
                UPDATE public.lotes_treino
                SET status = 'CONCLUIDO',
                    mensagem_erro = NULL,
                    finalizado_em = ?,
                    artifacts_id = ?,
                    mae_kfold = ?,
                    rmse_kfold = ?,
                    r2_kfold = ?
                WHERE id = ?
                """,
                Timestamp.from(Instant.now()),
                response.getArtifactsId(),
                mae,
                rmse,
                r2,
                inputId
        );
    }

    private void markFailed(Long inputId, RuntimeException exception) {
        String message = exception.getMessage() != null ? exception.getMessage() : "Falha ao treinar modelo";
        jdbcTemplate.update("""
                UPDATE public.lotes_treino
                SET status = 'FALHA',
                    mensagem_erro = ?,
                    finalizado_em = ?
                WHERE id = ?
                """,
                message,
                Timestamp.from(Instant.now()),
                inputId
        );
    }

    private void validateAvailableDatasets(Long companyId, List<Long> inputIds) {
        Object[] args = new Object[inputIds.size() + 1];
        args[0] = companyId;
        for (int index = 0; index < inputIds.size(); index++) {
            args[index + 1] = inputIds.get(index);
        }

        List<Long> foundIds = jdbcTemplate.queryForList("""
                SELECT id
                FROM public.lotes_treino
                WHERE company_id = ?
                  AND status = 'DISPONIVEL'
                  AND id IN (%s)
                """.formatted(placeholders(inputIds.size())), Long.class, args);

        if (foundIds.size() != inputIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uma ou mais bases selecionadas não existem, não pertencem à company ou não estão disponíveis");
        }
    }

    private void ensureDatasetAccess(Long inputId, Long companyId) {
        if (inputId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inputId é obrigatório");
        }

        String sql = """
                SELECT COUNT(*)
                FROM public.lotes_treino
                WHERE id = ?
                  AND status = 'DISPONIVEL'
                """;

        Integer found;
        if (companyId != null) {
            found = jdbcTemplate.queryForObject(sql + " AND company_id = ?", Integer.class, inputId, companyId);
        } else {
            found = jdbcTemplate.queryForObject(sql, Integer.class, inputId);
        }

        if (found == null || found == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Base de dados não encontrada");
        }
    }

    private int countRowsForDatasets(List<Long> inputIds) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.dados_treino_operacional
                WHERE input_id IN (%s)
                """.formatted(placeholders(inputIds.size())), Integer.class, inputIds.toArray());
        return count != null ? count : 0;
    }

    private int countTrainingRows(Long inputId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.dados_treino_operacional WHERE input_id = ?",
                Integer.class,
                inputId
        );
        return count != null ? count : 0;
    }

    private List<Long> normalizeInputIds(List<Long> inputIds) {
        if (inputIds == null) {
            return List.of();
        }

        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long inputId : inputIds) {
            if (inputId != null && inputId > 0) {
                normalized.add(inputId);
            }
        }
        return new ArrayList<>(normalized);
    }

    private String placeholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }

    private String joinInputIds(List<Long> inputIds) {
        return inputIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private Double numberAsDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Double requiredDouble(Map<String, Object> record, String key) {
        Object value = record.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Valor ausente: " + key);
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.valueOf(value.toString());
    }

    private Integer requiredInteger(Map<String, Object> record, String key) {
        Object value = record.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Valor ausente: " + key);
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private String requiredString(Map<String, Object> record, String key) {
        Object value = record.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Valor ausente: " + key);
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Valor ausente: " + key);
        }
        return text;
    }
}
