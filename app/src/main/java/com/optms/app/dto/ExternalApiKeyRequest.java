package com.optms.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExternalApiKeyRequest {

    @NotBlank
    private String customName;

    private Long companyId;
}
