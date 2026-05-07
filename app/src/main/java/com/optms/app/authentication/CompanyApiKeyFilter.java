package com.optms.app.authentication;

import com.optms.app.model.Company;
import com.optms.app.repository.CompanyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CompanyApiKeyFilter extends OncePerRequestFilter {

    public static final String COMPANY_ID_ATTRIBUTE = "authenticatedCompanyId";
    private static final String API_KEY_HEADER = "X-API-KEY";

    private final CompanyRepository companyRepository;

    public CompanyApiKeyFilter(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (!path.startsWith("/api/")) {
            return true;
        }

        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            return true;
        }

        return "/api/empresas".equals(path) && "POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("API Key da company não informada");
            return;
        }

        Company company = companyRepository.findByApikeyAndActiveTrue(apiKey)
                .orElse(null);

        if (company == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("API Key da company inválida");
            return;
        }

        request.setAttribute(COMPANY_ID_ATTRIBUTE, company.getId());
        filterChain.doFilter(request, response);
    }
}
