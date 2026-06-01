package com.optms.app.repository;

import com.optms.app.model.TabelaFrete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TabelaFreteRepository extends JpaRepository<TabelaFrete, Long> {

    Optional<TabelaFrete> findFirstByCompanyIdAndUfOrigemAndAtivaTrueOrderByIdDesc(Long companyId, String ufOrigem);

    List<TabelaFrete> findByCompanyIdAndUfOrigemAndAtivaTrueOrderByIdDesc(Long companyId, String ufOrigem);

    List<TabelaFrete> findByCompanyId(Long companyId);

    List<TabelaFrete> findByCompanyIdAndAtivaTrue(Long companyId);

    List<TabelaFrete> findByCompanyIdAndAtivaTrueOrderByIdDesc(Long companyId);

    Optional<TabelaFrete> findByIdAndCompanyId(Long id, Long companyId);
}
