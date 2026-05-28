package com.optms.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "objeto_frete")
public class ObjetoFrete {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tabela_id")
    private Long tabelaId;

    @Column(name = "uf_origem", length = 2)
    private String ufOrigem;

    @Column(name = "uf_destino", length = 2)
    private String ufDestino;

    @Column(name = "tipo_objeto")
    private String tipoObjeto;

    @Column(name = "nome")
    private String nomeComponente;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_calculo", columnDefinition = "jsonb")
    private ConfiguracaoCalculoFrete configCalculo;
}
