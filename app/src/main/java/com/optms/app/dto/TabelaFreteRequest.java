package com.optms.app.dto;

import com.optms.app.model.ConfiguracaoCalculoFrete;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Payload para criação de uma tabela de frete com seus componentes. */
@Getter
@Setter
public class TabelaFreteRequest {

    /** UFs de origem que esta tabela atende (ex.: ["SP", "MG"]). */
    private List<String> ufsOrigem;

    private String nome;
    private LocalDate vigenciaInicio;
    private LocalDate vigenciaFim;
    private boolean ativa = true;

    /** Lista de componentes de frete (PARTIDA + COMPONENTEs). */
    private List<ObjetoFreteDto> objetos;

    @Getter
    @Setter
    public static class ObjetoFreteDto {

        private String ufOrigem;
        private String ufDestino;

        /** PARTIDA = frete base; COMPONENTE = encargo adicional. */
        private String tipoObjeto;

        private String nomeComponente;

        private ConfiguracaoCalculoFrete configCalculo;
    }
}
