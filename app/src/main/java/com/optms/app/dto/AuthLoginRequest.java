package com.optms.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthLoginRequest {

    @NotBlank
    private String login;

    @NotBlank
    private String password;
}
