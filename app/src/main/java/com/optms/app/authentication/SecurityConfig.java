package com.optms.app.authentication;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final MasterApiKey masterApiKey;
    private final CompanyApiKeyFilter companyApiKeyFilter;
    private final CotacaoApiKeyFilter cotacaoApiKeyFilter;

    public SecurityConfig(
            MasterApiKey masterApiKey,
            CompanyApiKeyFilter companyApiKeyFilter,
            CotacaoApiKeyFilter cotacaoApiKeyFilter
    ) {
        this.masterApiKey = masterApiKey;
        this.companyApiKeyFilter = companyApiKeyFilter;
        this.cotacaoApiKeyFilter = cotacaoApiKeyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Geralmente desabilitado para APIs stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .addFilterBefore(masterApiKey, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(cotacaoApiKeyFilter, MasterApiKey.class)
            .addFilterAfter(companyApiKeyFilter, MasterApiKey.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
