package com.optms.app.controller.api;

import com.optms.app.authentication.CompanyApiKeyFilter;
import com.optms.app.authentication.MasterApiKey;
import com.optms.app.model.ExternalApiKey;
import com.optms.app.repository.ExternalApiKeyRepository;
import com.optms.app.service.ExternalApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/api/external-apikeys")
@RequiredArgsConstructor
public class ExternalApiKeyController {

    private final ExternalApiKeyRepository externalApiKeyRepository;
    private final ExternalApiKeyService externalApiKeyService;

    @PostMapping
    public ResponseEntity<ExternalApiKey> criar(@RequestBody ExternalApiKey externalApiKey, HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        externalApiKey = externalApiKeyService.criar(externalApiKey, companyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(externalApiKey);
    }

    @GetMapping
    public ResponseEntity<?> listar(HttpServletRequest request, @RequestParam(required = false) String nome) {
        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            if (nome != null && !nome.isBlank()) {
                return ResponseEntity.ok(externalApiKeyRepository.findByCustomNameContainingIgnoreCase(nome));
            }
            return ResponseEntity.ok(externalApiKeyRepository.findAll());
        }

        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        if (nome != null && !nome.isBlank()) {
            return ResponseEntity.ok(externalApiKeyRepository.findByCompanyIdAndCustomNameContainingIgnoreCase(companyId, nome));
        }
        return ResponseEntity.ok(externalApiKeyRepository.findByCompanyId(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExternalApiKey> obterPorId(@PathVariable Long id, HttpServletRequest request) {
        Optional<ExternalApiKey> externalApiKey;

        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            externalApiKey = externalApiKeyRepository.findById(id);
        } else {
            Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
            externalApiKey = externalApiKeyService.obterPorIdECompany(id, companyId);
        }

        return ResponseEntity.ok(externalApiKey.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "External API key não encontrada")));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<ExternalApiKey> desativar(@PathVariable Long id, HttpServletRequest request) {
        ExternalApiKey externalApiKey;

        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            externalApiKey = externalApiKeyService.desativarPorId(id);
        } else {
            Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
            externalApiKey = externalApiKeyService.desativarPorIdECompany(id, companyId);
        }

        return ResponseEntity.ok(externalApiKey);
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<ExternalApiKey> ativar(@PathVariable Long id, HttpServletRequest request) {
        ExternalApiKey externalApiKey;

        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            externalApiKey = externalApiKeyService.ativarPorId(id);
        } else {
            Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
            externalApiKey = externalApiKeyService.ativarPorIdECompany(id, companyId);
        }

        return ResponseEntity.ok(externalApiKey);
    }
}
