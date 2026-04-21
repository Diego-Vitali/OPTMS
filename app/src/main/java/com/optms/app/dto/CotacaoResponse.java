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

    /** Valor do frete base calculado pelo objeto PARTIDA. */
    private Double freteBase;

    /** Encargos adicionais aplicados ao embarque. */
    private List<ComponenteItem> componentes;

    /** Valor total do frete (freteBase + soma dos componentes). */
    private Double total;

    /** Um encargo adicional detalhado (nome e valor calculado). */
    public record ComponenteItem(String nome, Double valor) {}
}
