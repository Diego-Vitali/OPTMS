package com.optms.app.dto;

import com.optms.app.model.ObjetoFrete;
import com.optms.app.model.TabelaFrete;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TabelaFreteDetalheResponse {
    private TabelaFrete tabela;
    private List<ObjetoFrete> objetos;
}
