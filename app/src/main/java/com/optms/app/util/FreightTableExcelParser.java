package com.optms.app.util;

import com.optms.app.dto.TabelaFreteRequest;
import com.optms.app.dto.TabelaFreteRequest.ObjetoFreteDto;
import com.optms.app.model.FaixaCalculo;
import com.optms.app.model.FaixaCalculo.Regra;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.RecordFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class FreightTableExcelParser {

    private static final int XLSX_BYTE_ARRAY_MAX_OVERRIDE = 300_000_000;
    private static final String WILDCARD = "*";

    private static final Set<String> PARTIDA_HEADERS = Set.of(
            "UF_ORIGEM",
            "UF_DESTINO",
            "FAIXA_INICIAL",
            "FAIXA_FINAL",
            "UNIDADE_VARIANTE",
            "METODO_DE_CALCULO",
            "VALOR"
    );

    private static final Set<String> COMPONENTE_HEADERS = Set.of(
            "NOME_COMPONENTE",
            "UF_ORIGEM",
            "UF_DESTINO",
            "FAIXA_INICIAL",
            "FAIXA_FINAL",
            "UNIDADE_VARIANTE",
            "METODO_DE_CALCULO",
            "VALOR"
    );

    public TabelaFreteRequest parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo xlsx não informado");
        }

        IOUtils.setByteArrayMaxOverride(XLSX_BYTE_ARRAY_MAX_OVERRIDE);

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet configSheet = findSheet(workbook, "config");
            Sheet partidaSheet = findSheet(workbook, "frete_partida");
            Sheet componenteSheet = findSheet(workbook, "componente");

            if (configSheet == null || partidaSheet == null || componenteSheet == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O arquivo deve conter as abas 'config', 'frete_partida' e 'componente'");
            }

            Map<String, String> config = parseConfig(configSheet);
            String identifier = requiredConfig(config, "IDENTIFICADOR_DA_TABELA");

            List<ObjetoFreteDto> objetos = new ArrayList<>();
            objetos.addAll(parseObjects(partidaSheet, true));
            objetos.addAll(parseObjects(componenteSheet, false));

            if (objetos.stream().noneMatch(objeto -> "PARTIDA".equals(objeto.getTipoObjeto()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A aba frete_partida precisa ter pelo menos uma regra válida");
            }

            TabelaFreteRequest request = new TabelaFreteRequest();
            request.setUfOrigem(resolveLegacyOrigin(objetos));
            request.setNome(identifier);
            request.setTipo(requiredConfig(config, "TIPO"));
            request.setVigenciaInicio(parseDate(requiredConfig(config, "VIGENCIA_INICIO"), "VIGENCIA_INICIO"));
            request.setVigenciaFim(parseDate(requiredConfig(config, "VIGENCIA_FIM"), "VIGENCIA_FIM"));
            request.setAtiva(true);
            request.setObjetos(objetos);
            return request;
        } catch (RecordFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O arquivo xlsx excede o limite interno de leitura ou está corrompido", exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado", exception);
        }
    }

    private Map<String, String> parseConfig(Sheet configSheet) {
        Map<String, String> config = new LinkedHashMap<>();
        for (int index = configSheet.getFirstRowNum(); index <= configSheet.getLastRowNum(); index++) {
            Row row = configSheet.getRow(index);
            if (ExcelSupport.isBlank(row)) {
                continue;
            }

            String key = ExcelSupport.text(row, Map.of("KEY", 0), "KEY");
            String value = ExcelSupport.text(row, Map.of("VALUE", 1), "VALUE");
            if (key != null && value != null) {
                config.put(ExcelSupport.normalizeKey(key), value.trim());
            }
        }
        return config;
    }

    private List<ObjetoFreteDto> parseObjects(Sheet sheet, boolean partida) {
        Map<String, Integer> headers = ExcelSupport.mapHeaders(sheet.getRow(sheet.getFirstRowNum()));
        ExcelSupport.requireHeaders(headers, partida ? PARTIDA_HEADERS : COMPONENTE_HEADERS);

        Map<String, ObjetoFreteDto> objectsByKey = new LinkedHashMap<>();
        for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
            Row row = sheet.getRow(index);
            if (ExcelSupport.isBlank(row)) {
                continue;
            }

            String ufOrigem = normalizeUf(requiredText(row, headers, "UF_ORIGEM"));
            String ufDestino = normalizeUf(requiredText(row, headers, "UF_DESTINO"));
            String unidade = normalizeUnidade(requiredText(row, headers, "UNIDADE_VARIANTE"), partida);
            String metodo = normalizeMetodo(requiredText(row, headers, "METODO_DE_CALCULO"));
            String nome = partida ? "FRETE_PARTIDA" : requiredText(row, headers, "NOME_COMPONENTE").trim();
            String politicaExcedente = normalizePoliticaExcedente(ExcelSupport.text(row, headers, "POLITICA_EXCEDENTE"));

            Double faixaInicial = ExcelSupport.decimal(row, headers, "FAIXA_INICIAL");
            Double faixaFinal = ExcelSupport.decimal(row, headers, "FAIXA_FINAL");
            if ((faixaInicial == null) != (faixaFinal == null)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "FAIXA_INICIAL e FAIXA_FINAL devem ser preenchidas juntas ou deixadas vazias");
            }
            if (faixaInicial != null && faixaFinal < faixaInicial) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAIXA_FINAL não pode ser menor que FAIXA_INICIAL");
            }

            Regra regra = new Regra();
            regra.setFaixaInicial(faixaInicial);
            regra.setFaixaFinal(faixaFinal);
            regra.setValor(requiredDecimal(row, headers, "VALOR"));

            String key = String.join("|", partida ? "PARTIDA" : "COMPONENTE", nome, ufOrigem, ufDestino, unidade, metodo);
            ObjetoFreteDto objeto = objectsByKey.computeIfAbsent(key, ignored -> buildObject(partida, nome, ufOrigem, ufDestino, unidade, metodo, politicaExcedente));
            if (isLastRangeForObject(objeto, regra)) {
                objeto.getFaixas().setPoliticaExcedente(politicaExcedente);
            }
            objeto.getFaixas().getRegras().add(regra);
        }

        return new ArrayList<>(objectsByKey.values());
    }

    private boolean isLastRangeForObject(ObjetoFreteDto objeto, Regra candidate) {
        if (candidate.getFaixaFinal() == null) {
            return true;
        }
        return objeto.getFaixas().getRegras().stream()
                .map(Regra::getFaixaFinal)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(Double.NEGATIVE_INFINITY) <= candidate.getFaixaFinal();
    }

    private ObjetoFreteDto buildObject(
            boolean partida,
            String nome,
            String ufOrigem,
            String ufDestino,
            String unidade,
            String metodo,
            String politicaExcedente
    ) {
        FaixaCalculo faixas = new FaixaCalculo();
        faixas.setTipoFaixa(unidade);
        faixas.setUnidadeVariante(unidade);
        faixas.setMetodoCalculo(metodo);
        faixas.setPoliticaExcedente(politicaExcedente);
        faixas.setRegras(new ArrayList<>());

        ObjetoFreteDto dto = new ObjetoFreteDto();
        dto.setTipoObjeto(partida ? "PARTIDA" : "COMPONENTE");
        dto.setNomeComponente(nome);
        dto.setUfOrigem(ufOrigem);
        dto.setUfDestino(ufDestino);
        dto.setUf(WILDCARD.equals(ufDestino) ? null : ufDestino);
        dto.setBaseCalculo(unidade);
        dto.setTipoCalculo(metodo);
        dto.setSobreFretePartida(FaixaCalculo.UNIDADE_FRETE_PARTIDA.equals(unidade));
        dto.setFaixas(faixas);
        return dto;
    }

    private Sheet findSheet(Workbook workbook, String expectedName) {
        String expected = ExcelSupport.normalizeKey(expectedName);
        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            Sheet sheet = workbook.getSheetAt(index);
            if (ExcelSupport.normalizeKey(sheet.getSheetName()).equals(expected)) {
                return sheet;
            }
        }
        return null;
    }

    private String requiredConfig(Map<String, String> config, String key) {
        String value = config.get(ExcelSupport.normalizeKey(key));
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Config obrigatório ausente: " + key);
        }
        return value.trim();
    }

    private String requiredText(Row row, Map<String, Integer> headers, String header) {
        String value = ExcelSupport.text(row, headers, header);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor obrigatório ausente na coluna " + header);
        }
        return value;
    }

    private Double requiredDecimal(Row row, Map<String, Integer> headers, String header) {
        Double value = ExcelSupport.decimal(row, headers, header);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor numérico inválido na coluna " + header);
        }
        return value;
    }

    private String normalizeUf(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return "TODOS".equals(normalized) || "ALL".equals(normalized) ? WILDCARD : normalized;
    }

    private String normalizeUnidade(String value, boolean partida) {
        String normalized = ExcelSupport.normalizeKey(value);
        String unidade = switch (normalized) {
            case "PESO", "KG" -> FaixaCalculo.UNIDADE_PESO;
            case "VLR", "VALOR", "VALOR_NF", "VLR_NF" -> FaixaCalculo.UNIDADE_VALOR;
            case "FRETE_PARTIDA", "PARTIDA" -> FaixaCalculo.UNIDADE_FRETE_PARTIDA;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNIDADE_VARIANTE inválida: " + value);
        };
        if (partida && FaixaCalculo.UNIDADE_FRETE_PARTIDA.equals(unidade)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "frete_partida não é uma unidade válida na aba frete_partida");
        }
        return unidade;
    }

    private String normalizeMetodo(String value) {
        String trimmed = value.trim();
        if ("*".equals(trimmed)) {
            return FaixaCalculo.METODO_MULTIPLICADOR;
        }
        if ("%".equals(trimmed)) {
            return FaixaCalculo.METODO_PERCENTUAL;
        }

        String normalized = ExcelSupport.normalizeKey(value);
        return switch (normalized) {
            case "MULTIPLICADOR" -> FaixaCalculo.METODO_MULTIPLICADOR;
            case "PERCENTUAL", "PORCENTAGEM", "PCT" -> FaixaCalculo.METODO_PERCENTUAL;
            case "VLR_FIXO", "VALOR_FIXO", "FIXO" -> FaixaCalculo.METODO_VALOR_FIXO;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "METODO_DE_CALCULO inválido: " + value);
        };
    }

    private String normalizePoliticaExcedente(String value) {
        if (value == null || value.isBlank()) {
            return FaixaCalculo.EXCEDENTE_TOTAL;
        }
        String normalized = ExcelSupport.normalizeKey(value);
        return switch (normalized) {
            case "APENAS_EXCEDENTE", "EXCEDENTE" -> FaixaCalculo.EXCEDENTE_APENAS_EXCEDENTE;
            case "TOTAL", "TUDO" -> FaixaCalculo.EXCEDENTE_TOTAL;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "POLITICA_EXCEDENTE inválida: " + value);
        };
    }

    private LocalDate parseDate(String value, String field) {
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("M/d/yyyy")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data inválida em " + field + ": " + value);
    }

    private String resolveLegacyOrigin(List<ObjetoFreteDto> objetos) {
        List<String> origins = objetos.stream()
                .map(ObjetoFreteDto::getUfOrigem)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        return origins.size() == 1 ? origins.getFirst() : "MULTI";
    }
}
