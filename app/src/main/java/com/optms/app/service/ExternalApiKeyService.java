package com.optms.app.service;

import com.optms.app.model.ExternalApiKey;
import com.optms.app.repository.ExternalApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExternalApiKeyService {

    private final ExternalApiKeyRepository externalApiKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ExternalApiKey criar(ExternalApiKey externalApiKey, Long companyId) {
        externalApiKey.setCompanyId(companyId);
        externalApiKey.setApikey(generateUniqueApiKey());

        if (externalApiKey.getActive() == null) {
            externalApiKey.setActive(true);
        }

        return externalApiKeyRepository.save(externalApiKey);
    }

    public Optional<ExternalApiKey> obterPorIdECompany(Long id, Long companyId) {
        return externalApiKeyRepository.findByIdAndCompanyId(id, companyId);
    }

    @Transactional
    public ExternalApiKey desativarPorId(Long id) {
        ExternalApiKey externalApiKey = externalApiKeyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "External API key não encontrada"));

        externalApiKey.setActive(false);
        return externalApiKeyRepository.save(externalApiKey);
    }

    @Transactional
    public ExternalApiKey desativarPorIdECompany(Long id, Long companyId) {
        ExternalApiKey externalApiKey = externalApiKeyRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "External API key não encontrada"));

        externalApiKey.setActive(false);
        return externalApiKeyRepository.save(externalApiKey);
    }

    @Transactional
    public ExternalApiKey ativarPorId(Long id) {
        ExternalApiKey externalApiKey = externalApiKeyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "External API key não encontrada"));

        externalApiKey.setActive(true);
        return externalApiKeyRepository.save(externalApiKey);
    }

    @Transactional
    public ExternalApiKey ativarPorIdECompany(Long id, Long companyId) {
        ExternalApiKey externalApiKey = externalApiKeyRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "External API key não encontrada"));

        externalApiKey.setActive(true);
        return externalApiKeyRepository.save(externalApiKey);
    }

    private String generateUniqueApiKey() {
        byte[] bytes = new byte[16];
        String apiKey;

        do {
            secureRandom.nextBytes(bytes);
            apiKey = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (externalApiKeyRepository.existsByApikey(apiKey));

        return apiKey;
    }
}
