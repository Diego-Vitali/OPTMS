package com.optms.app.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FaixaCalculo {
    private String tipoFaixa; // VLR_NF ou PESO
    private List<Double> faixas; // [10, 25, 50, 100]
    private List<Double> valores; // [50, 65, 80, 120]
    private Double valorExcedente;
     
    // Método utilitário para o motor de cálculo
    public Double buscarValor(Double entrada) {
        for (int i = 0; i < faixas.size(); i++) {
            if (entrada <= faixas.get(i)) {
                return valores.get(i);
            }
        }
        return valorExcedente;
    }
}
