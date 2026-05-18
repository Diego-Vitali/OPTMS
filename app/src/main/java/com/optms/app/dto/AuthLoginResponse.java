package com.optms.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthLoginResponse {

    private Long userId;
    private String name;
    private String email;
    private Long companyId;
    private String companyName;
    private String apiKey;
    private boolean admin;
}
