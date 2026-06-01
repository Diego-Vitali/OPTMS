package com.optms.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MlTrainRequest {

    @JsonAlias("company_id")
    private Long companyId;

    @JsonAlias("input_ids")
    private List<Long> inputIds;
}
