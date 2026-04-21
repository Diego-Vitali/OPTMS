package com.optms.app.dto;

import lombok.Getter;
import lombok.Setter;

/** Dados de entrada para cálculo de uma cotação de frete. */
@Getter
@Setter
public class CotacaoRequest {

    /** UF de origem da mercadoria (ex.: "SP"). */
    private String ufOrigem;

    /** UF de destino da mercadoria (ex.: "RJ"). */
    private String ufDestino;

    /** Peso bruto total do embarque em quilogramas. */
    private Double peso;

    /** Valor total das notas fiscais em reais. */
    private Double valorNF;
}
