package com.optms.app.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

public final class ExcelSupport {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.US);

    private ExcelSupport() {
    }

    public static Map<String, Integer> mapHeaders(Row headerRow) {
        if (headerRow == null) {
            return Map.of();
        }

        Map<String, Integer> headers = new HashMap<>();
        for (Cell cell : headerRow) {
            String rawValue = DATA_FORMATTER.formatCellValue(cell);
            if (rawValue != null && !rawValue.isBlank()) {
                headers.put(normalizeKey(rawValue), cell.getColumnIndex());
            }
        }
        return headers;
    }

    public static void requireHeaders(Map<String, Integer> headers, Set<String> requiredHeaders) {
        for (String header : requiredHeaders) {
            if (!headers.containsKey(normalizeKey(header))) {
                throw new IllegalArgumentException("Coluna obrigatória não encontrada: " + header);
            }
        }
    }

    public static String text(Row row, Map<String, Integer> headers, String header) {
        Integer index = headers.get(normalizeKey(header));
        if (index == null) {
            return null;
        }

        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }

        String value = DATA_FORMATTER.formatCellValue(cell);
        return value != null ? value.trim() : null;
    }

    public static Double decimal(Row row, Map<String, Integer> headers, String header) {
        Integer index = headers.get(normalizeKey(header));
        if (index == null) {
            return null;
        }

        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }

        String value = DATA_FORMATTER.formatCellValue(cell);
        if (value == null || value.isBlank()) {
            return null;
        }

        String sanitized = value.trim();
        if (sanitized.contains(",") && sanitized.contains(".")) {
            sanitized = sanitized.replace(".", "").replace(",", ".");
        } else if (sanitized.contains(",")) {
            sanitized = sanitized.replace(",", ".");
        }

        try {
            return Double.parseDouble(sanitized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static Integer integer(Row row, Map<String, Integer> headers, String header) {
        Double value = decimal(row, headers, header);
        return value != null ? value.intValue() : null;
    }

    public static boolean isBlank(Row row) {
        if (row == null) {
            return true;
        }

        for (Cell cell : row) {
            String value = DATA_FORMATTER.formatCellValue(cell);
            if (value != null && !value.isBlank()) {
                return false;
            }
        }

        return true;
    }

    public static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .toUpperCase(Locale.ROOT);
        return normalized.replaceAll("_+", "_").replaceAll("^_|_$", "");
    }
}
