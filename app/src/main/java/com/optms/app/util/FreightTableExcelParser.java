package com.optms.app.util;

import com.optms.app.dto.TabelaFreteRequest;
import com.optms.app.dto.TabelaFreteRequest.ObjetoFreteDto;
import com.optms.app.model.FaixaCalculo;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "UF_ORIGEM",
            "UF_DESTINO",
            "FAIXA_INICIAL",
            "FAIXA_FINAL",
            "VALOR_FRETE",
            "PESO_MINIMO",
            "GRIS",
            "AD_VALOREM",
            "PEDAGIO"
    );

    public TabelaFreteRequest parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo xlsx não informado");
        }

        IOUtils.setByteArrayMaxOverride(XLSX_BYTE_ARRAY_MAX_OVERRIDE);

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet configSheet = workbook.getSheet("Config");
            Sheet dataSheet = workbook.getSheet("Tabela");

            if (configSheet == null || dataSheet == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo deve conter as abas 'Config' e 'Tabela'");
            }

            Map<String, String> config = parseConfig(configSheet);
            Map<String, Integer> headers = ExcelSupport.mapHeaders(dataSheet.getRow(dataSheet.getFirstRowNum()));
            ExcelSupport.requireHeaders(headers, REQUIRED_HEADERS);

            List<FreightRow> rows = new ArrayList<>();
            for (int index = dataSheet.getFirstRowNum() + 1; index <= dataSheet.getLastRowNum(); index++) {
                Row row = dataSheet.getRow(index);
                if (ExcelSupport.isBlank(row)) {
                    continue;
                }

                rows.add(new FreightRow(
                        normalizeUf(requiredText(row, headers, "UF_ORIGEM")),
                        normalizeUf(requiredText(row, headers, "UF_DESTINO")),
                        requiredDecimal(row, headers, "FAIXA_INICIAL"),
                        requiredDecimal(row, headers, "FAIXA_FINAL"),
                        requiredDecimal(row, headers, "VALOR_FRETE"),
                        ExcelSupport.decimal(row, headers, "PESO_MINIMO"),
                        zeroIfNull(ExcelSupport.decimal(row, headers, "GRIS")),
                        zeroIfNull(ExcelSupport.decimal(row, headers, "AD_VALOREM")),
                        zeroIfNull(ExcelSupport.decimal(row, headers, "PEDAGIO"))
                ));
            }

            if (rows.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhuma linha válida encontrada na aba Tabela");
            }

            List<String> origins = rows.stream()
                    .map(FreightRow::ufOrigem)
                    .distinct()
                    .toList();
            if (origins.size() != 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O upload deve conter apenas uma UF de origem por arquivo");
            }

            String rangeType = normalizeRangeType(config.get("TIPO"));
            String tableName = config.getOrDefault("TRANSPORTADORA", "Tabela " + origins.getFirst());

            TabelaFreteRequest request = new TabelaFreteRequest();
            request.setUfOrigem(origins.getFirst());
            request.setNome(tableName);
            request.setAtiva(true);
            request.setObjetos(buildObjects(rows, rangeType));
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

    private List<ObjetoFreteDto> buildObjects(List<FreightRow> rows, String rangeType) {
        Map<String, List<FreightRow>> rowsByDestination = rows.stream()
                .sorted(Comparator.comparing(FreightRow::faixaFinal))
                .collect(Collectors.groupingBy(FreightRow::ufDestino, LinkedHashMap::new, Collectors.toList()));

        List<ObjetoFreteDto> objects = new ArrayList<>();
        for (Map.Entry<String, List<FreightRow>> entry : rowsByDestination.entrySet()) {
            String destination = entry.getKey();
            List<FreightRow> destinationRows = entry.getValue();

            objects.add(buildFixedComponent(destination, "PARTIDA", "FRETE_BASE", rangeType, "FAIXA", false, destinationRows,
                    destinationRows.stream().map(FreightRow::valorFrete).toList(),
                    destinationRows.stream().map(FreightRow::pesoMinimo).filter(value -> value != null && value > 0).findFirst().orElse(null)));

            if (destinationRows.stream().anyMatch(row -> row.gris() > 0)) {
                objects.add(buildFixedComponent(destination, "COMPONENTE", "GRIS", "VLR_NF", "PERCENTUAL", false, destinationRows,
                        destinationRows.stream().map(FreightRow::gris).toList(), null));
            }

            if (destinationRows.stream().anyMatch(row -> row.adValorem() > 0)) {
                objects.add(buildFixedComponent(destination, "COMPONENTE", "AD_VALOREM", "VLR_NF", "PERCENTUAL", false, destinationRows,
                        destinationRows.stream().map(FreightRow::adValorem).toList(), null));
            }

            if (destinationRows.stream().anyMatch(row -> row.pedagio() > 0)) {
                objects.add(buildFixedComponent(destination, "COMPONENTE", "PEDAGIO", rangeType, "FAIXA", false, destinationRows,
                        destinationRows.stream().map(FreightRow::pedagio).toList(), null));
            }
        }

        return objects;
    }

    private ObjetoFreteDto buildFixedComponent(
            String destination,
            String objectType,
            String componentName,
            String calculationBase,
            String calculationType,
            boolean overBaseFreight,
            List<FreightRow> rows,
            List<Double> values,
            Double minimumValue
    ) {
        FaixaCalculo faixaCalculo = new FaixaCalculo();
        faixaCalculo.setTipoFaixa(calculationBase);
        faixaCalculo.setFaixasIniciais(rows.stream().map(FreightRow::faixaInicial).toList());
        faixaCalculo.setFaixas(rows.stream().map(FreightRow::faixaFinal).toList());
        faixaCalculo.setValores(values);
        faixaCalculo.setValorExcedente(values.getLast());
        faixaCalculo.setMinimo(minimumValue);

        ObjetoFreteDto dto = new ObjetoFreteDto();
        dto.setUf(destination);
        dto.setTipoObjeto(objectType);
        dto.setBaseCalculo(calculationBase);
        dto.setTipoCalculo(calculationType);
        dto.setNomeComponente(componentName);
        dto.setSobreFretePartida(overBaseFreight);
        dto.setFaixas(faixaCalculo);
        return dto;
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
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRangeType(String value) {
        return "VALOR".equalsIgnoreCase(value) ? "VLR_NF" : "PESO";
    }

    private double zeroIfNull(Double value) {
        return value != null ? value : 0.0;
    }

    private record FreightRow(
            String ufOrigem,
            String ufDestino,
            Double faixaInicial,
            Double faixaFinal,
            Double valorFrete,
            Double pesoMinimo,
            Double gris,
            Double adValorem,
            Double pedagio
    ) {
    }
}
