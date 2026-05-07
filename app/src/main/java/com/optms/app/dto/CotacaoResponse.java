package com.optms.app.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Resultado de uma cotação de frete com detalhamento por componente. */
@Getter
@Builder
public class CotacaoResponse {

    private String ufOrigem;
    private String ufDestino;
    private Double peso;
    private Double valorNF;

    /** Resultado da cotação calculada para cada tabela ativa encontrada. */
    private List<TabelaCotacaoItem> cotacoes;

    /** Um encargo adicional detalhado (nome e valor calculado). */
    public record ComponenteItem(String nome, Double valor) {}

    public record TabelaCotacaoItem(
            Long tabelaId,
            String tabelaNome,
            Double freteBase,
            List<ComponenteItem> componentes,
            Double total
    ) {}
}
