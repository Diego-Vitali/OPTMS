package com.optms.app.repository;

import com.optms.app.model.ExternalApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalApiKeyRepository extends JpaRepository<ExternalApiKey, Long> {

    List<ExternalApiKey> findByCompanyId(Long companyId);

    List<ExternalApiKey> findByCustomNameContainingIgnoreCase(String customName);

    List<ExternalApiKey> findByCompanyIdAndCustomNameContainingIgnoreCase(Long companyId, String customName);

    List<ExternalApiKey> findByCompanyIdAndActiveTrue(Long companyId);

    Optional<ExternalApiKey> findByIdAndCompanyId(Long id, Long companyId);

    Optional<ExternalApiKey> findByApikeyAndActiveTrue(String apikey);

    boolean existsByApikey(String apikey);
}
