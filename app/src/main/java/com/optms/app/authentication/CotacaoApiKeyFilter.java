package com.optms.app.authentication;

import com.optms.app.model.Company;
import com.optms.app.model.ExternalApiKey;
import com.optms.app.repository.CompanyRepository;
import com.optms.app.repository.ExternalApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CotacaoApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final CompanyRepository companyRepository;
    private final ExternalApiKeyRepository externalApiKeyRepository;

    public CotacaoApiKeyFilter(
            CompanyRepository companyRepository,
            ExternalApiKeyRepository externalApiKeyRepository
    ) {
        this.companyRepository = companyRepository;
        this.externalApiKeyRepository = externalApiKeyRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            return true;
        }

        String path = request.getRequestURI();
        boolean supportsExternalApiKey = "/api/cotacoes".equals(path)
                || "/api/ml/predict".equals(path);

        return !supportsExternalApiKey || !"POST".equalsIgnoreCase(request.getMethod());
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
            response.getWriter().write("API Key não informada");
            return;
        }

        Company company = companyRepository.findByApikeyAndActiveTrue(apiKey).orElse(null);
        if (company != null) {
            request.setAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE, company.getId());
            filterChain.doFilter(request, response);
            return;
        }

        ExternalApiKey externalApiKey = externalApiKeyRepository.findByApikeyAndActiveTrue(apiKey).orElse(null);
        if (externalApiKey == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("API Key inválida para cotação");
            return;
        }

        Company externalCompany = companyRepository.findByIdAndActiveTrue(externalApiKey.getCompanyId()).orElse(null);
        if (externalCompany == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Company da API key externa está inativa");
            return;
        }

        request.setAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE, externalCompany.getId());
        filterChain.doFilter(request, response);
    }
}
