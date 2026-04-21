package com.optms.app.controller.api;

import com.optms.app.dto.TabelaFreteRequest;
import com.optms.app.model.TabelaFrete;
import com.optms.app.service.TabelaFreteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tabelas-frete")
@RequiredArgsConstructor
@Tag(name = "Tabelas de Frete", description = "Cadastro de tabelas de frete por UF de origem")
public class TabelaFreteController {

    private final TabelaFreteService tabelaFreteService;

    @PostMapping
    @Operation(
        summary = "Criar tabela de frete",
        description = "Cadastra uma nova tabela de frete com seus componentes (PARTIDA + COMPONENTEs) para uma UF de origem."
    )
    @ApiResponse(responseCode = "201", description = "Tabela criada com sucesso")
    @ApiResponse(responseCode = "400", description = "Payload inválido")
    public ResponseEntity<TabelaFrete> criar(@RequestBody TabelaFreteRequest request) {
        TabelaFrete saved = tabelaFreteService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
