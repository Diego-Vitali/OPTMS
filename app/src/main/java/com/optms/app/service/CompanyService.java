package com.optms.app.service;

import com.optms.app.model.Company;
import com.optms.app.model.ExternalApiKey;
import com.optms.app.repository.CompanyRepository;
import com.optms.app.repository.ExternalApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final ExternalApiKeyRepository externalApiKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public Company criar(Company company) {
        company.setApikey(generateUniqueApiKey());

        if (company.getActive() == null) {
            company.setActive(true);
        }

        return companyRepository.save(company);
    }

    @Transactional
    public Company desativarPorId(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company não encontrada"));

        company.setActive(false);
        desativarExternalApiKeys(company.getId());

        return companyRepository.save(company);
    }

    @Transactional
    public Company ativarPorId(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company não encontrada"));

        company.setActive(true);

        return companyRepository.save(company);
    }

    public Optional<Company> obterPorIdECompany(Long id, Long companyId) {
        if (!id.equals(companyId)) {
            return Optional.empty();
        }

        return companyRepository.findByIdAndActiveTrue(id);
    }

    private void desativarExternalApiKeys(Long companyId) {
        List<ExternalApiKey> externalApiKeys = externalApiKeyRepository.findByCompanyIdAndActiveTrue(companyId);
        for (ExternalApiKey externalApiKey : externalApiKeys) {
            externalApiKey.setActive(false);
        }
        externalApiKeyRepository.saveAll(externalApiKeys);
    }

    private String generateUniqueApiKey() {
        byte[] bytes = new byte[16];
        String apiKey;

        do {
            secureRandom.nextBytes(bytes);
            apiKey = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (companyRepository.existsByApikey(apiKey));

        return apiKey;
    }
}
