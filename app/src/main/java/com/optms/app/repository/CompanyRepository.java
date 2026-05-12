package com.optms.app.repository;

import com.optms.app.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByApikeyAndActiveTrue(String apikey);

    Optional<Company> findByIdAndActiveTrue(Long id);

    List<Company> findByNameContainingIgnoreCase(String name);

    boolean existsByApikey(String apikey);
}
