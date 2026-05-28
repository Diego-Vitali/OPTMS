package com.optms.app.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TabelaFreteUploadResponse {

    private Long tabelaId;
    private String nome;
    private List<String> ufsOrigem;
    private int objetosCriados;
    private String message;
}
