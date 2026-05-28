package com.optms.app.model;

import com.optms.app.model.converter.FaixaCalculoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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

    @Column(name = "uf")
    private String uf;

    @Column(name = "tipo_objeto")
    private String tipoObjeto;

    @Column(name = "base_calculo")
    private String baseCalculo;

    @Column(name = "tipo_calculo")
    private String tipoCalculo;

    @Column(name = "nome")
    private String nomeComponente;

    @Column(name = "sobre_frete_partida")
    private boolean sobreFretePartida;

    @Convert(converter = FaixaCalculoConverter.class)
    @Column(name = "config_faixas", columnDefinition = "jsonb")
    private FaixaCalculo faixas;
}
