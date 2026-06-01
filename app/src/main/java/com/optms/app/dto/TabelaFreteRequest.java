package com.optms.app.dto;

import com.optms.app.model.FaixaCalculo;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/** Payload para criação de uma tabela de frete com seus componentes. */
@Getter
@Setter
public class TabelaFreteRequest {

    /** UF do estado de origem que esta tabela atende (ex.: "SP"). */
    private String ufOrigem;

    private String nome;
    private String tipo;
    private LocalDate vigenciaInicio;
    private LocalDate vigenciaFim;
    private boolean ativa = true;

    /** Lista de componentes de frete (PARTIDA + COMPONENTEs). */
    private List<ObjetoFreteDto> objetos;

    @Getter
    @Setter
    public static class ObjetoFreteDto {

        /** UF de destino para COMPONENTEs; null = aplica a todos os destinos. */
        private String uf;
        private String ufOrigem;
        private String ufDestino;

        /** PARTIDA = frete base; COMPONENTE = encargo adicional. */
        private String tipoObjeto;

        private String baseCalculo;
        private String tipoCalculo;
        private String nomeComponente;

        /** Se verdadeiro, o componente é calculado sobre o valor do frete base (PARTIDA). */
        private boolean sobreFretePartida;

        private FaixaCalculo faixas;
    }
}
