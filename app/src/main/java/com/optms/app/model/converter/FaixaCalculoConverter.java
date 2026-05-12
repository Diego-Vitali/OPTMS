package com.optms.app.model.converter;

import com.optms.app.model.FaixaCalculo;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Converter
public class FaixaCalculoConverter implements AttributeConverter<FaixaCalculo, String> {

    @Override
    public String convertToDatabaseColumn(FaixaCalculo attr) {
        if (attr == null) return null;
        return "{" +
            "\"tipoFaixa\":" + jsonString(attr.getTipoFaixa()) + "," +
            "\"faixas\":" + jsonDoubleArray(attr.getFaixas()) + "," +
            "\"valores\":" + jsonDoubleArray(attr.getValores()) + "," +
            "\"valorExcedente\":" + attr.getValorExcedente() +
        "}";
    }

    @Override
    public FaixaCalculo convertToEntityAttribute(String data) {
        if (data == null || data.isBlank()) return null;
        FaixaCalculo fc = new FaixaCalculo();
        fc.setTipoFaixa(extractString(data, "tipoFaixa"));
        fc.setFaixas(extractDoubleArray(data, "faixas"));
        fc.setValores(extractDoubleArray(data, "valores"));
        fc.setValorExcedente(extractDouble(data, "valorExcedente"));
        return fc;
    }

    private String jsonString(String value) {
        return value == null ? "null" : "\"" + value.replace("\"", "\\\"") + "\"";
    }

    private String jsonDoubleArray(List<Double> list) {
        if (list == null) return "null";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
        }
        return sb.append("]").toString();
    }

    private String extractString(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private Double extractDouble(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*([\\d.]+)").matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : null;
    }

    private List<Double> extractDoubleArray(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json);
        if (!m.find()) return new ArrayList<>();
        List<Double> result = new ArrayList<>();
        for (String part : m.group(1).split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) result.add(Double.parseDouble(trimmed));
        }
        return result;
    }
}
