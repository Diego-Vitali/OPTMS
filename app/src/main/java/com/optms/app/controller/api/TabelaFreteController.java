package com.optms.app.controller.api;

import com.optms.app.authentication.CompanyApiKeyFilter;
import com.optms.app.dto.TabelaFreteRequest;
import com.optms.app.model.TabelaFrete;
import com.optms.app.service.TabelaFreteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.optms.app.authentication.MasterApiKey;

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
    public ResponseEntity<TabelaFrete> criar(@RequestBody TabelaFreteRequest request, HttpServletRequest httpRequest) {
        Long companyId = (Long) httpRequest.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        TabelaFrete saved = tabelaFreteService.criar(request, companyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<TabelaFrete> desativar(@PathVariable Long id, HttpServletRequest request) {
        TabelaFrete tabela;

        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            tabela = tabelaFreteService.desativarPorId(id);
        } else {
            Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
            tabela = tabelaFreteService.desativarPorIdECompany(id, companyId);
        }

        return ResponseEntity.ok(tabela);
    }
}
