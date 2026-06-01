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
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String path = normalizedPath(request);
        String apiKey = request.getHeader(API_KEY_HEADER);
        boolean isAdminRoute = path.startsWith("/api/admin/");

        boolean hasMasterKey = apiKey != null && apiKey.equals(authToken);
        boolean isCompanyCreation = "/api/empresas".equals(path) && "POST".equalsIgnoreCase(method);
        boolean isCompanyUpdate = path.startsWith("/api/empresas/")
                && ("PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method));
        boolean isCompanyStatusEndpoint = path.startsWith("/api/empresas/")
                && "PATCH".equalsIgnoreCase(method)
                && (path.endsWith("/desativar") || path.endsWith("/ativar"));
        boolean routeRequiresMaster = isAdminRoute
                || isCompanyCreation
                || isCompanyUpdate
                || isCompanyStatusEndpoint;

        if (hasMasterKey) {
            request.setAttribute(MASTER_ACCESS_ATTRIBUTE, true);
        }

        if (routeRequiresMaster && !hasMasterKey) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("API Key inválida");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String normalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
