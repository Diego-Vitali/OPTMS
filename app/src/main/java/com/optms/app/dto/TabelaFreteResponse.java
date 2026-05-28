package com.optms.app.dto;

import com.optms.app.model.ConfiguracaoCalculoFrete;
import java.time.LocalDate;
import java.util.List;

public record TabelaFreteResponse(
        Long id,
        Long companyId,
        String nome,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        boolean ativa,
        List<String> ufsOrigem,
        List<String> ufsDestino,
        List<ObjetoFreteResponse> objetos
) {
    public record ObjetoFreteResponse(
            Long id,
            String tipoObjeto,
            String nomeComponente,
            String ufOrigem,
            String ufDestino,
            ConfiguracaoCalculoFrete configCalculo
    ) {
    }
}
