package com.optms.app.controller.api;

import com.optms.app.authentication.CompanyApiKeyFilter;
import com.optms.app.dto.CotacaoRequest;
import com.optms.app.dto.CotacaoResponse;
import com.optms.app.service.CotacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cotacoes")
@RequiredArgsConstructor
@Tag(name = "Cotações", description = "Cálculo de cotações de frete fracionado (LTL)")
public class CotacaoController {

    private final CotacaoService cotacaoService;

    @PostMapping
    @Operation(
        summary = "Calcular cotação de frete",
        description = """
            Localiza a tabela de frete ativa para a UF de origem, calcula o frete base (PARTIDA)
            e soma os encargos adicionais (COMPONENTEs) aplicáveis à UF de destino.
            Retorna o detalhamento por componente e o valor total.
            """,
        parameters = @Parameter(
            name = "X-API-KEY",
            in = ParameterIn.HEADER,
            required = true,
            description = "API key interna ou API key externa ativa associada à empresa"
        )
    )
    @ApiResponse(
        responseCode = "200",
        description = "Cotação calculada com sucesso",
        content = @Content(schema = @Schema(implementation = CotacaoResponse.class))
    )
    @ApiResponse(responseCode = "404", description = "Nenhuma tabela ativa para a UF de origem")
    @ApiResponse(responseCode = "422", description = "Tabela sem objeto PARTIDA cadastrado")
    public ResponseEntity<CotacaoResponse> calcular(@RequestBody CotacaoRequest request, HttpServletRequest httpRequest) {
        Long companyId = (Long) httpRequest.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        return ResponseEntity.ok(cotacaoService.calcular(request, companyId));
    }
}
