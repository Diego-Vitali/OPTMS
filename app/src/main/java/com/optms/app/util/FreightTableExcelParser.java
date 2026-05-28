package com.optms.app.util;

import com.optms.app.dto.TabelaFreteRequest;
import com.optms.app.dto.TabelaFreteRequest.ObjetoFreteDto;
import com.optms.app.model.ConfiguracaoCalculoFrete;
import com.optms.app.model.RegraCalculo;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class FreightTableExcelParser {

    private static final Set<String> CONFIG_HEADERS = Set.of(
            "NOME_REFERENCIA",
            "VIGENCIA_INICIO",
            "VIGENCIA_FIM"
    );

    private static final Set<String> RULE_HEADERS = Set.of(
            "UF_ORIGEM",
            "UF_DESTINO",
            "FORMA_CALCULO",
            "UNIDADE_FAIXA",
            "LIMITE_INICIAL",
            "LIMITE_FINAL",
            "UNIDADE_VARIANTE",
            "TIPO_CALCULO",
            "VALOR_DO_CALCULO"
    );

    private static final Set<String> COMPONENT_HEADERS = Set.of(
            "UF_ORIGEM",
            "UF_DESTINO",
            "NOME_COMPONENTE",
            "FORMA_CALCULO",
            "UNIDADE_FAIXA",
            "LIMITE_INICIAL",
            "LIMITE_FINAL",
            "UNIDADE_VARIANTE",
            "TIPO_CALCULO",
            "VALOR_DO_CALCULO"
    );

    public TabelaFreteRequest parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo xlsx não informado");
        }

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet configSheet = requireSheet(workbook, "Config");
            Sheet baseFreightSheet = requireSheet(workbook, "Frete_Partida");
            Sheet componentsSheet = workbook.getSheet("Componentes");

            ConfigRow config = parseConfig(configSheet);
            List<FreightRuleRow> rows = new ArrayList<>();
            rows.addAll(parseRuleSheet(baseFreightSheet, "PARTIDA", false));
            if (componentsSheet != null) {
                rows.addAll(parseRuleSheet(componentsSheet, "COMPONENTE", true));
            }

            if (rows.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Nenhuma linha válida encontrada nas abas Frete_Partida e Componentes");
            }

            TabelaFreteRequest request = new TabelaFreteRequest();
            request.setNome(config.nomeReferencia());
            request.setVigenciaInicio(config.vigenciaInicio());
            request.setVigenciaFim(config.vigenciaFim());
            request.setAtiva(true);
            LinkedHashSet<String> ufsOrigem = rows.stream()
                    .map(FreightRuleRow::ufOrigem)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            request.setUfsOrigem(new ArrayList<>(ufsOrigem));
            request.setObjetos(buildObjects(rows));
            return request;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado", exception);
        }
    }

    private Sheet requireSheet(Workbook workbook, String name) {
        Sheet sheet = workbook.getSheet(name);
        if (sheet == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo deve conter a aba '" + name + "'");
        }
        return sheet;
    }

    private ConfigRow parseConfig(Sheet sheet) {
        Map<String, Integer> headers = ExcelSupport.mapHeaders(sheet.getRow(sheet.getFirstRowNum()));
        ExcelSupport.requireHeaders(headers, CONFIG_HEADERS);

        for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
            Row row = sheet.getRow(index);
            if (ExcelSupport.isBlank(row)) {
                continue;
            }
            return new ConfigRow(
                    requiredText(row, headers, "NOME_REFERENCIA"),
                    readDate(row, headers, "VIGENCIA_INICIO"),
                    readDate(row, headers, "VIGENCIA_FIM")
            );
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A aba Config deve conter uma linha de configuração");
    }

    private List<FreightRuleRow> parseRuleSheet(Sheet sheet, String objectType, boolean componentSheet) {
        Map<String, Integer> headers = ExcelSupport.mapHeaders(sheet.getRow(sheet.getFirstRowNum()));
        ExcelSupport.requireHeaders(headers, componentSheet ? COMPONENT_HEADERS : RULE_HEADERS);

        List<FreightRuleRow> rows = new ArrayList<>();
        for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
            Row row = sheet.getRow(index);
            if (ExcelSupport.isBlank(row)) {
                continue;
            }
            rows.add(parseRuleRow(row, headers, objectType, componentSheet));
        }
        return rows;
    }

    private FreightRuleRow parseRuleRow(Row row, Map<String, Integer> headers, String objectType, boolean componentSheet) {
        String ufOrigem = normalizeUf(requiredText(row, headers, "UF_ORIGEM"));
        String ufDestino = normalizeUf(requiredText(row, headers, "UF_DESTINO"));
        String nomeComponente = componentSheet ? requiredText(row, headers, "NOME_COMPONENTE") : "Frete partida";
        String formaCalculo = normalizeToken(requiredText(row, headers, "FORMA_CALCULO"));
        String unidadeFaixa = normalizeToken(ExcelSupport.text(row, headers, "UNIDADE_FAIXA"));
        String unidadeVariante = normalizeToken(requiredText(row, headers, "UNIDADE_VARIANTE"));
        String tipoCalculo = normalizeToken(requiredText(row, headers, "TIPO_CALCULO"));
        Double valorCalculo = requiredDecimal(row, headers, "VALOR_DO_CALCULO");

        RegraCalculo regra = new RegraCalculo();
        regra.setLimiteInicial(ExcelSupport.decimal(row, headers, "LIMITE_INICIAL"));
        regra.setLimiteFinal(ExcelSupport.decimal(row, headers, "LIMITE_FINAL"));
        regra.setUnidadeVariante(unidadeVariante);
        regra.setTipoCalculo(tipoCalculo);
        regra.setValorCalculo(valorCalculo);

        return new FreightRuleRow(
                ufOrigem,
                ufDestino,
                objectType,
                nomeComponente.trim(),
                formaCalculo,
                unidadeFaixa,
                regra
        );
    }

    private List<ObjetoFreteDto> buildObjects(List<FreightRuleRow> rows) {
        Map<ObjectKey, List<FreightRuleRow>> rowsByObject = rows.stream()
                .collect(Collectors.groupingBy(FreightRuleRow::objectKey, LinkedHashMap::new, Collectors.toList()));

        List<ObjetoFreteDto> objects = new ArrayList<>();
        for (Map.Entry<ObjectKey, List<FreightRuleRow>> entry : rowsByObject.entrySet()) {
            ObjectKey key = entry.getKey();
            List<FreightRuleRow> objectRows = entry.getValue();

            ConfiguracaoCalculoFrete config = new ConfiguracaoCalculoFrete();
            config.setFormaCalculo(key.formaCalculo());
            config.setUnidadeFaixa(key.unidadeFaixa());
            config.setRegras(objectRows.stream().map(FreightRuleRow::regra).toList());

            ObjetoFreteDto dto = new ObjetoFreteDto();
            dto.setTipoObjeto(key.tipoObjeto());
            dto.setNomeComponente(key.nomeComponente());
            dto.setUfOrigem(key.ufOrigem());
            dto.setUfDestino(key.ufDestino());
            dto.setConfigCalculo(config);
            objects.add(dto);
        }

        return objects;
    }

    private String requiredText(Row row, Map<String, Integer> headers, String header) {
        String value = ExcelSupport.text(row, headers, header);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor obrigatório ausente na coluna " + header);
        }
        return value;
    }

    private LocalDate readDate(Row row, Map<String, Integer> headers, String header) {
        Integer index = headers.get(ExcelSupport.normalizeKey(header));
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        String value = ExcelSupport.text(row, headers, header);
        if (value == null || value.isBlank()) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data inválida na coluna " + header);
    }

    private Double requiredDecimal(Row row, Map<String, Integer> headers, String header) {
        Double value = ExcelSupport.decimal(row, headers, header);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor numérico inválido na coluna " + header);
        }
        return value;
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUf(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record ConfigRow(
            String nomeReferencia,
            LocalDate vigenciaInicio,
            LocalDate vigenciaFim
    ) {
    }

    private record FreightRuleRow(
            String ufOrigem,
            String ufDestino,
            String tipoObjeto,
            String nomeComponente,
            String formaCalculo,
            String unidadeFaixa,
            RegraCalculo regra
    ) {
        private ObjectKey objectKey() {
            return new ObjectKey(ufOrigem, ufDestino, tipoObjeto, nomeComponente, formaCalculo, unidadeFaixa);
        }
    }

    private record ObjectKey(
            String ufOrigem,
            String ufDestino,
            String tipoObjeto,
            String nomeComponente,
            String formaCalculo,
            String unidadeFaixa
    ) {
    }
}
