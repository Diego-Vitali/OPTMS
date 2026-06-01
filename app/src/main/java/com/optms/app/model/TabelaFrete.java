package com.optms.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Tabela de frete ativa para um conjunto de rotas. */
@Getter
@Setter
@Entity
@Table(name = "tabela_frete")
public class TabelaFrete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /** Valor legado para listagem. As rotas reais ficam nos objetos de frete. */
    @Column(name = "uf_origem", length = 20)
    private String ufOrigem;

    @Column(name = "nome")
    private String nome;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "vigencia_inicio")
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    /** Indica se a tabela está vigente e deve ser usada nas cotações. */
    @Column(name = "ativa")
    private boolean ativa;
}
