package com.optms.app.util;

import java.io.IOException;
import java.io.InputStream;
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
public class TrainingExcelParser {

    private static final int XLSX_BYTE_ARRAY_MAX_OVERRIDE = 300_000_000;

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "UF_ORIGEM",
            "UF_DESTINO",
            "PESO_BRUTO",
            "METRO_CUBICO",
            "VALOR_NF",
            "QTD_VOLUMES",
            "VIA_TRANSPORTE",
            "TIPO_FRETE",
            "TRANSIT_TIME_REAL"
    );

    public TrainingParseResult parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo xlsx não informado");
        }

        IOUtils.setByteArrayMaxOverride(XLSX_BYTE_ARRAY_MAX_OVERRIDE);

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Planilha de treinamento não encontrada");
            }

            Map<String, Integer> headers = ExcelSupport.mapHeaders(sheet.getRow(sheet.getFirstRowNum()));
            ExcelSupport.requireHeaders(headers, REQUIRED_HEADERS);

            List<Map<String, Object>> records = new ArrayList<>();
            int discardedRows = 0;
            for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (ExcelSupport.isBlank(row)) {
                    continue;
                }

                Map<String, Object> record = new LinkedHashMap<>();
                record.put("Peso total bruto", ExcelSupport.decimal(row, headers, "PESO_BRUTO"));
                record.put("Metro cúbico", ExcelSupport.decimal(row, headers, "METRO_CUBICO"));
                record.put("Valor NF", ExcelSupport.decimal(row, headers, "VALOR_NF"));
                record.put("Volume NF", ExcelSupport.integer(row, headers, "QTD_VOLUMES"));
                record.put("Tipo de frete NF", normalizeFreightType(ExcelSupport.text(row, headers, "TIPO_FRETE")));
                record.put("Via de transporte", normalizeTransportMode(ExcelSupport.text(row, headers, "VIA_TRANSPORTE")));
                record.put("UF emitente NF", normalizeUf(ExcelSupport.text(row, headers, "UF_ORIGEM")));
                record.put("UF destinatário NF", normalizeUf(ExcelSupport.text(row, headers, "UF_DESTINO")));
                record.put("transit time", ExcelSupport.integer(row, headers, "TRANSIT_TIME_REAL"));

                if (!isValidTrainingRecord(record)) {
                    discardedRows++;
                    continue;
                }

                records.add(record);
            }

            if (records.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum registro de treinamento foi encontrado");
            }

            return new TrainingParseResult(records, discardedRows);
        } catch (RecordFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O arquivo xlsx excede o limite interno de leitura ou está corrompido", exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado", exception);
        }
    }

    private String normalizeUf(String value) {
        return value != null ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalizeFreightType(String value) {
        if (value == null) {
            return null;
        }

        String normalized = ExcelSupport.normalizeKey(value);
        return switch (normalized) {
            case "CIF" -> "CIF";
            case "FOB" -> "FOB";
            default -> value.trim();
        };
    }

    private String normalizeTransportMode(String value) {
        if (value == null) {
            return null;
        }

        return switch (ExcelSupport.normalizeKey(value)) {
            case "RODOVIARIO" -> "Rodoviário";
            case "AEREO" -> "Aéreo";
            case "MARITIMO" -> "Marítimo";
            case "FERROVIARIO" -> "Ferroviário";
            case "CABOTAGEM" -> "Cabotagem";
            default -> value.trim();
        };
    }

    private boolean isValidTrainingRecord(Map<String, Object> record) {
        return record.get("Peso total bruto") instanceof Number
                && record.get("Metro cúbico") instanceof Number
                && record.get("Valor NF") instanceof Number
                && record.get("Volume NF") instanceof Number
                && record.get("transit time") instanceof Number
                && hasText(record.get("Tipo de frete NF"))
                && hasText(record.get("Via de transporte"))
                && hasText(record.get("UF emitente NF"))
                && hasText(record.get("UF destinatário NF"));
    }

    private boolean hasText(Object value) {
        return value != null && !value.toString().isBlank();
    }

    public record TrainingParseResult(List<Map<String, Object>> records, int discardedRows) {
    }
}
