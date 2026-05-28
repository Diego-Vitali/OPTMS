package com.optms.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Tabela de frete ativa para uma ou mais UFs de origem. */
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

    /** UFs de origem que esta tabela atende (ex.: ["SP", "MG"]). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ufs_origem", nullable = false, columnDefinition = "jsonb")
    private List<String> ufsOrigem = new ArrayList<>();

    @Column(name = "nome")
    private String nome;

    @Column(name = "vigencia_inicio")
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    /** Indica se a tabela está vigente e deve ser usada nas cotações. */
    @Column(name = "ativa")
    private boolean ativa;
}
