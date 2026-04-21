package com.optms.app.repository;

import com.optms.app.model.TabelaFrete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TabelaFreteRepository extends JpaRepository<TabelaFrete, Long> {

    /** Retorna a tabela ativa mais recente para a UF de origem informada. */
    Optional<TabelaFrete> findFirstByUfOrigemAndAtivaTrueOrderByIdDesc(String ufOrigem);
}
