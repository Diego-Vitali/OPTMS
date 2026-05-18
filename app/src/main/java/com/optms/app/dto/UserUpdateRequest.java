package com.optms.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @Size(min = 6, max = 120)
    private String password;

    private Long companyId;
}
