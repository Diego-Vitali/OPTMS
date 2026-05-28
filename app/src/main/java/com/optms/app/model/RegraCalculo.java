package com.optms.app.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegraCalculo {

    private Double limiteInicial;
    private Double limiteFinal;
    private String unidadeVariante;
    private String tipoCalculo;
    private Double valorCalculo;
}
