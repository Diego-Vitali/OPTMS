package com.optms.app.controller.api;

import com.optms.app.dto.CotacaoRequest;
import com.optms.app.dto.CotacaoResponse;
import com.optms.app.service.CotacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cotacoes")
@RequiredArgsConstructor
public class AdminCotacaoController {

    private final CotacaoService cotacaoService;

    @PostMapping
    public ResponseEntity<CotacaoResponse> calcular(
            @RequestParam Long companyId,
            @RequestBody CotacaoRequest request
    ) {
        return ResponseEntity.ok(cotacaoService.calcular(request, companyId));
    }
}
