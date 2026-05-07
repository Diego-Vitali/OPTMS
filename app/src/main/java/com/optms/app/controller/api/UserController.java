package com.optms.app.controller.api;

import com.optms.app.authentication.CompanyApiKeyFilter;
import com.optms.app.authentication.MasterApiKey;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.optms.app.model.User;
import com.optms.app.repository.UserRepository;
import com.optms.app.service.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UserController {
 
    private final UserRepository userRepository;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<User> criar(@RequestBody User user, HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        user.setCompanyId(companyId);
        user = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping
    public ResponseEntity<Iterable<User>> listar(HttpServletRequest request, @RequestParam(required = false) String nome) {
        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            if (nome != null && !nome.isBlank()) {
                return ResponseEntity.ok(userRepository.findByNameContainingIgnoreCase(nome));
            }
            return ResponseEntity.ok(userRepository.findAll());
        }

        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        Iterable<User> users;

        if (nome != null && !nome.isBlank()) {
            users = userRepository.findByCompanyIdAndNameContainingIgnoreCase(companyId, nome);
        } else {
            users = userRepository.findByCompanyId(companyId);
        }

        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<User> desativar(@PathVariable Long id, HttpServletRequest request) {
        User user;

        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            user = userService.desativarPorId(id);
        } else {
            Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
            user = userService.desativarPorIdECompany(id, companyId);
        }

        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<User> ativar(@PathVariable Long id, HttpServletRequest request) {
        User user;

        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            user = userService.ativarPorId(id);
        } else {
            Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
            user = userService.ativarPorIdECompany(id, companyId);
        }

        return ResponseEntity.ok(user);
    }
}
