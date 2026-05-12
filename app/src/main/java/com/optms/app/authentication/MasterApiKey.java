package com.optms.app.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class MasterApiKey extends OncePerRequestFilter {

    public static final String MASTER_ACCESS_ATTRIBUTE = "masterAccess";
    private static final String API_KEY_HEADER = "X-API-KEY";
    private final String authToken;

    public MasterApiKey(@Value("${master.api.key}") String authToken) {
        this.authToken = authToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean isCompanyCreation = "/api/empresas".equals(path) && "POST".equalsIgnoreCase(method);
        boolean isApiGet = path.startsWith("/api/") && "GET".equalsIgnoreCase(method);
        boolean isCompanyStatusEndpoint = path.startsWith("/api/empresas/")
                && "PATCH".equalsIgnoreCase(method)
                && (path.endsWith("/desativar") || path.endsWith("/ativar"));
        boolean isUserStatusEndpoint = path.startsWith("/api/usuarios/")
                && "PATCH".equalsIgnoreCase(method)
                && (path.endsWith("/desativar") || path.endsWith("/ativar"));
        boolean isExternalApiKeyStatusEndpoint = path.startsWith("/api/external-apikeys/")
                && "PATCH".equalsIgnoreCase(method)
                && (path.endsWith("/desativar") || path.endsWith("/ativar"));

        return !isCompanyCreation
                && !isApiGet
                && !isCompanyStatusEndpoint
                && !isUserStatusEndpoint
                && !isExternalApiKeyStatusEndpoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String apiKey = request.getHeader(API_KEY_HEADER);

        if ("POST".equalsIgnoreCase(method)) {
            if (apiKey == null || !apiKey.equals(authToken)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("API Key inválida");
                return;
            }

            filterChain.doFilter(request, response);
            return;
        }

        if (apiKey != null && apiKey.equals(authToken)) {
            request.setAttribute(MASTER_ACCESS_ATTRIBUTE, true);
        }

        filterChain.doFilter(request, response);
    }
}
