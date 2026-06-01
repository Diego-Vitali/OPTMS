package com.optms.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MlRetrainUploadResponse {

    private String status;
    private String error;
    private String message;

    @JsonProperty("company_id")
    @JsonAlias("companyId")
    private Long companyId;

    @JsonProperty("input_id")
    @JsonAlias("inputId")
    private Long inputId;

    @JsonProperty("artifacts_id")
    @JsonAlias("artifactsId")
    private String artifactsId;

    @JsonAlias("n_registros_treino")
    private Integer nRegistrosTreino;

    @JsonAlias("linhas_descartadas")
    private Integer linhasDescartadas;

    @JsonAlias("linhas_saudaveis")
    private Integer linhasSaudaveis;

    @JsonAlias("mae_kfold")
    private Double maeKfold;

    @JsonAlias("rmse_kfold")
    private Double rmseKfold;

    @JsonAlias("r2_kfold")
    private Double r2Kfold;

    private Map<String, Object> metrics;

    @JsonAlias("info_modelo")
    private Map<String, Object> infoModelo;
}
