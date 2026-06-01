package com.optms.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FaixaCalculo {
    public static final String UNIDADE_PESO = "PESO";
    public static final String UNIDADE_VALOR = "VLR_NF";
    public static final String UNIDADE_FRETE_PARTIDA = "FRETE_PARTIDA";
    public static final String METODO_MULTIPLICADOR = "MULTIPLICADOR";
    public static final String METODO_PERCENTUAL = "PERCENTUAL";
    public static final String METODO_VALOR_FIXO = "VALOR_FIXO";
    public static final String EXCEDENTE_TOTAL = "TOTAL";
    public static final String EXCEDENTE_APENAS_EXCEDENTE = "APENAS_EXCEDENTE";

    private String unidadeVariante;
    private String metodoCalculo;
    private String politicaExcedente = EXCEDENTE_TOTAL;
    private List<Regra> regras;

    private String tipoFaixa; // VLR_NF ou PESO
    private List<Double> faixasIniciais; // Valores mínimos informados no xlsx
    private List<Double> faixas; // [10, 25, 50, 100]
    private List<Double> valores; // [50, 65, 80, 120]
    private Double valorExcedente;
    private Double minimo;

    public Double buscarValor(Double entrada) {
        if (entrada == null || faixas == null || valores == null || faixas.isEmpty() || valores.isEmpty()) {
            return null;
        }

        for (int i = 0; i < faixas.size(); i++) {
            if (entrada <= faixas.get(i)) {
                return valores.get(i);
            }
        }
        return valorExcedente != null ? valorExcedente : valores.getLast();
    }

    public Optional<RegraAplicavel> buscarRegra(Double entrada) {
        if (entrada == null || regras == null || regras.isEmpty()) {
            return Optional.empty();
        }

        Optional<Regra> constante = regras.stream()
                .filter(Regra::isConstante)
                .findFirst();
        if (constante.isPresent()) {
            return Optional.of(new RegraAplicavel(constante.get(), entrada, false));
        }

        for (Regra regra : regras.stream()
                .sorted(Comparator.comparing(Regra::faixaFinalOrMax))
                .toList()) {
            if (regra.contem(entrada)) {
                return Optional.of(new RegraAplicavel(regra, entrada, false));
            }
        }

        Regra ultima = regras.stream()
                .filter(regra -> regra.getFaixaFinal() != null)
                .max(Comparator.comparing(Regra::getFaixaFinal))
                .orElse(null);

        if (ultima == null || entrada <= ultima.getFaixaFinal()) {
            return Optional.empty();
        }

        double baseExcedente = EXCEDENTE_APENAS_EXCEDENTE.equalsIgnoreCase(String.valueOf(politicaExcedente))
                ? entrada - ultima.getFaixaFinal()
                : entrada;
        return Optional.of(new RegraAplicavel(ultima, baseExcedente, true));
    }

    @JsonIgnore
    public boolean usaNovaEstrutura() {
        return regras != null && !regras.isEmpty();
    }

    @Getter
    @Setter
    public static class Regra {
        private Double faixaInicial;
        private Double faixaFinal;
        private Double valor;

        @JsonIgnore
        public boolean isConstante() {
            return faixaInicial == null && faixaFinal == null;
        }

        public boolean contem(Double entrada) {
            if (entrada == null) {
                return false;
            }
            if (isConstante()) {
                return true;
            }
            double inicial = faixaInicial != null ? faixaInicial : Double.NEGATIVE_INFINITY;
            double finalRange = faixaFinal != null ? faixaFinal : Double.POSITIVE_INFINITY;
            return entrada >= inicial && entrada <= finalRange;
        }

        private Double faixaFinalOrMax() {
            return faixaFinal != null ? faixaFinal : Double.MAX_VALUE;
        }
    }

    public record RegraAplicavel(Regra regra, Double baseCalculo, boolean excedente) {
    }
}
