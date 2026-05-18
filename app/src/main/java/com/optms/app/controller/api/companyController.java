package com.optms.app.controller.api;

import com.optms.app.authentication.CompanyApiKeyFilter;
import com.optms.app.authentication.MasterApiKey;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.optms.app.model.Company;
import com.optms.app.repository.CompanyRepository;
import com.optms.app.service.CompanyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class companyController {
 
    private final CompanyRepository companyRepository;
    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<Company> criar(@RequestBody Company company) {
        company = companyService.criar(company);
        return ResponseEntity.status(HttpStatus.CREATED).body(company);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Company> atualizar(@PathVariable Long id, @RequestBody Company company, HttpServletRequest request) {
        if (!Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas a master API key pode atualizar companies");
        }
        return ResponseEntity.ok(companyService.atualizar(id, company));
    }

    @GetMapping
    public ResponseEntity<?> listar(HttpServletRequest request, @RequestParam(required = false) String nome) {
        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            if (nome != null && !nome.isBlank()) {
                return ResponseEntity.ok(companyRepository.findByNameContainingIgnoreCase(nome));
            }
            return ResponseEntity.ok(companyRepository.findAll());
        }

        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);

        Company company = companyRepository.findByIdAndActiveTrue(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company não encontrada"));

        if (nome != null && !nome.isBlank() && !containsIgnoreCase(company.getName(), nome)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company não encontrada");
        }

        return ResponseEntity.ok(company);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Company> obterPorId(@PathVariable Long id, HttpServletRequest request) {
        Optional<Company> company;

        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            company = companyRepository.findById(id);
        } else {
            Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
            company = companyService.obterPorIdECompany(id, companyId);
        }

        return ResponseEntity.ok(company.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company não encontrada")));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Company> desativar(@PathVariable Long id, HttpServletRequest request) {
        if (!Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas a master API key pode desativar companies");
        }

        Company company = companyService.desativarPorId(id);
        return ResponseEntity.ok(company);
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Company> ativar(@PathVariable Long id, HttpServletRequest request) {
        if (!Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas a master API key pode ativar companies");
        }

        Company company = companyService.ativarPorId(id);
        return ResponseEntity.ok(company);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, HttpServletRequest request) {
        if (!Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas a master API key pode excluir companies");
        }

        companyService.excluirPorId(id);
        return ResponseEntity.noContent().build();
    }

    private boolean containsIgnoreCase(String value, String filtro) {
        return value != null && value.toLowerCase().contains(filtro.toLowerCase());
    }
}
