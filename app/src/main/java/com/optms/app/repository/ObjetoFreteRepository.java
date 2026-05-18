package com.optms.app.repository;

import com.optms.app.model.ObjetoFrete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObjetoFreteRepository extends JpaRepository<ObjetoFrete, Long> {

    /** Retorna todos os componentes vinculados a uma tabela de frete. */
    List<ObjetoFrete> findByTabelaId(Long tabelaId);

    void deleteByTabelaId(Long tabelaId);
}
