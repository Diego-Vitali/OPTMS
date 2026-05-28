package com.optms.app.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfiguracaoCalculoFrete {

    private String formaCalculo;
    private String unidadeFaixa;
    private List<RegraCalculo> regras = new ArrayList<>();

    public RegraCalculo buscarRegra(Double entrada) {
        if (regras == null || regras.isEmpty()) {
            return null;
        }

        if ("CONSTANTE".equalsIgnoreCase(formaCalculo)) {
            return regras.getFirst();
        }

        if (entrada == null) {
            return null;
        }

        return regras.stream()
                .filter(regra -> regra.getLimiteInicial() == null || entrada >= regra.getLimiteInicial())
                .filter(regra -> regra.getLimiteFinal() == null || entrada <= regra.getLimiteFinal())
                .findFirst()
                .orElse(null);
    }
}
