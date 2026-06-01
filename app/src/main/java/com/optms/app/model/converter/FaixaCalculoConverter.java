package com.optms.app.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optms.app.model.FaixaCalculo;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FaixaCalculoConverter implements AttributeConverter<FaixaCalculo, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public String convertToDatabaseColumn(FaixaCalculo attr) {
        if (attr == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(attr);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Não foi possível serializar a configuração de frete", exception);
        }
    }

    @Override
    public FaixaCalculo convertToEntityAttribute(String data) {
        if (data == null || data.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(data, FaixaCalculo.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Não foi possível ler a configuração de frete", exception);
        }
    }
}
