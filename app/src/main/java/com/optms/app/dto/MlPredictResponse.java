package com.optms.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MlPredictResponse {

    private String error;

    @JsonAlias("predicted_transit_time")
    private Double predictedTransitTime;
}
