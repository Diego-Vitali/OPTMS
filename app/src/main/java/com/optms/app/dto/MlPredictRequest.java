package com.optms.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MlPredictRequest {

    @JsonProperty("Peso total bruto")
    @JsonAlias("pesoTotalBruto")
    private Double pesoTotalBruto;

    @JsonProperty("Metro cúbico")
    @JsonAlias("metroCubico")
    private Double metroCubico;

    @JsonProperty("Valor NF")
    @JsonAlias("valorNF")
    private Double valorNF;

    @JsonProperty("Volume NF")
    @JsonAlias("volumeNF")
    private Integer volumeNF;

    @JsonProperty("Tipo de frete NF")
    @JsonAlias("tipoFreteNF")
    private String tipoFreteNF;

    @JsonProperty("Via de transporte")
    @JsonAlias("viaTransporte")
    private String viaTransporte;

    @JsonProperty("UF emitente NF")
    @JsonAlias("ufEmitenteNF")
    private String ufEmitenteNF;

    @JsonProperty("UF destinatário NF")
    @JsonAlias("ufDestinatarioNF")
    private String ufDestinatarioNF;
}
