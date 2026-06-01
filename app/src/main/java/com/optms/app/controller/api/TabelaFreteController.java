package com.optms.app.controller.api;

import com.optms.app.authentication.CompanyApiKeyFilter;
import com.optms.app.dto.TabelaFreteDetalheResponse;
import com.optms.app.dto.TabelaFreteRequest;
import com.optms.app.dto.TabelaFreteUploadResponse;
import com.optms.app.model.TabelaFrete;
import com.optms.app.service.TabelaFreteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.optms.app.authentication.MasterApiKey;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tabelas-frete")
@RequiredArgsConstructor
@Tag(name = "Tabelas de Frete", description = "Cadastro de tabelas de frete por UF de origem")
public class TabelaFreteController {

    private final TabelaFreteService tabelaFreteService;

    @GetMapping
    public ResponseEntity<List<TabelaFrete>> listar(HttpServletRequest httpRequest) {
        Long companyId = (Long) httpRequest.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        return ResponseEntity.ok(tabelaFreteService.listar(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TabelaFreteDetalheResponse> obter(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long companyId = (Long) httpRequest.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        return ResponseEntity.ok(tabelaFreteService.obterDetalhes(id, companyId));
    }

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

    @PostMapping("/upload-xlsx")
    public ResponseEntity<TabelaFreteUploadResponse> uploadXlsx(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest httpRequest
    ) {
        Long companyId = (Long) httpRequest.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        return ResponseEntity.status(HttpStatus.CREATED).body(tabelaFreteService.criarPorXlsx(file, companyId));
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

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<TabelaFrete> ativar(@PathVariable Long id, HttpServletRequest request) {
        TabelaFrete tabela;

        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            tabela = tabelaFreteService.ativarPorId(id);
        } else {
            Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
            tabela = tabelaFreteService.ativarPorIdECompany(id, companyId);
        }

        return ResponseEntity.ok(tabela);
    }
}
