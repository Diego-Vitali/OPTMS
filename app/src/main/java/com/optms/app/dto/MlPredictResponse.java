package com.optms.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlPredictResponse {

    private String error;

    private String risco;

    @JsonProperty("tma_estimado_dias")
    private Double tmaEstimadoDias;

    @JsonProperty("intervalo_sla_dias")
    private List<Integer> intervaloSlaDias;

    @JsonProperty("top_fatores_explicacao")
    private List<Map<String, Object>> topFatoresExplicacao;
}
