package com.optms.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TabelaFreteUploadResponse {

    private Long tabelaId;
    private String nome;
    private String ufOrigem;
    private int objetosCriados;
    private String message;
}
