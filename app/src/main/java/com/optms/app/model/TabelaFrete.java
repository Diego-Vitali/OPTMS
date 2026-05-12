package com.optms.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Tabela de frete ativa para uma UF de origem. */
@Getter
@Setter
@Entity
@Table(name = "tabela_frete")
public class TabelaFrete {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;

    /** ID do usuário que criou esta tabela. Deve ser passado pelo frontend. */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /** UF do estado de origem que esta tabela atende (ex.: "SP"). */
    @Column(name = "uf_origem", nullable = false, length = 2)
    private String ufOrigem;

    @Column(name = "nome")
    private String nome;

    /** Indica se a tabela está vigente e deve ser usada nas cotações. */
    @Column(name = "ativa")
    private boolean ativa;
}
