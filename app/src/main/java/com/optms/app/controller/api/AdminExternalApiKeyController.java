package com.optms.app.controller.api;

import com.optms.app.dto.ExternalApiKeyRequest;
import com.optms.app.model.ExternalApiKey;
import com.optms.app.service.ExternalApiKeyService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/external-apikeys")
@RequiredArgsConstructor
public class AdminExternalApiKeyController {

    private final ExternalApiKeyService externalApiKeyService;

    @GetMapping
    public ResponseEntity<List<ExternalApiKey>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Long companyId
    ) {
        return ResponseEntity.ok(externalApiKeyService.listarComoAdmin(companyId, nome));
    }

    @PostMapping
    public ResponseEntity<ExternalApiKey> criar(@Valid @RequestBody ExternalApiKeyRequest request) {
        if (request.getCompanyId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId é obrigatório");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(externalApiKeyService.criar(request.getCustomName(), request.getCompanyId()));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<ExternalApiKey> ativar(@PathVariable Long id) {
        return ResponseEntity.ok(externalApiKeyService.ativarPorId(id));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<ExternalApiKey> desativar(@PathVariable Long id) {
        return ResponseEntity.ok(externalApiKeyService.desativarPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        externalApiKeyService.excluirPorId(id);
        return ResponseEntity.noContent().build();
    }
}
