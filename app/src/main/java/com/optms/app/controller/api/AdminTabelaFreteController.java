package com.optms.app.controller.api;

import com.optms.app.dto.TabelaFreteUploadResponse;
import com.optms.app.dto.TabelaFreteDetalheResponse;
import com.optms.app.model.TabelaFrete;
import com.optms.app.service.TabelaFreteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/tabelas-frete")
@RequiredArgsConstructor
public class AdminTabelaFreteController {

    private final TabelaFreteService tabelaFreteService;

    @GetMapping
    public ResponseEntity<List<TabelaFrete>> listar(@RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(tabelaFreteService.listarComoAdmin(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TabelaFreteDetalheResponse> obter(@PathVariable Long id) {
        return ResponseEntity.ok(tabelaFreteService.obterDetalhesComoAdmin(id));
    }

    @PostMapping("/upload-xlsx")
    public ResponseEntity<TabelaFreteUploadResponse> uploadXlsx(
            @RequestPart("file") MultipartFile file,
            @RequestParam Long companyId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tabelaFreteService.criarPorXlsx(file, companyId));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<TabelaFrete> ativar(@PathVariable Long id) {
        return ResponseEntity.ok(tabelaFreteService.ativarPorId(id));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<TabelaFrete> desativar(@PathVariable Long id) {
        return ResponseEntity.ok(tabelaFreteService.desativarPorId(id));
    }
}
