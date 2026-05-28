package com.optms.app.repository;

import com.optms.app.model.TabelaFrete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TabelaFreteRepository extends JpaRepository<TabelaFrete, Long> {

    List<TabelaFrete> findByCompanyId(Long companyId);

    List<TabelaFrete> findByCompanyIdAndAtivaTrue(Long companyId);

    Optional<TabelaFrete> findByIdAndCompanyId(Long id, Long companyId);
}
