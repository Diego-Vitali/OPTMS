package com.optms.app.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "OPTMS — Transportation Management System",
                description = "API pública para cotação de frete fracionado (LTL) e previsão de prazo de entrega.",
                version = "1.0.0"
        ),
        security = @SecurityRequirement(name = "apiKey")
)
@SecurityScheme(
        name = "apiKey",
        description = "Informe sua API Key da empresa (X-API-KEY) para autenticar as requisições.",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-API-KEY"
)
public class OpenApiConfig {
}
