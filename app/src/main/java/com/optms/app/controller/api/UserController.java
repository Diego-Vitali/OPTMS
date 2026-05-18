package com.optms.app.controller.api;

import com.optms.app.authentication.CompanyApiKeyFilter;
import com.optms.app.authentication.MasterApiKey;
import com.optms.app.dto.UserRequest;
import com.optms.app.dto.UserUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    private static final String USER_ID_HEADER = "X-USER-ID";
 
    private final UserService userService;

    @PostMapping
    public ResponseEntity<User> criar(@Valid @RequestBody UserRequest userRequest, HttpServletRequest request) {
        Long companyId;

        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            if (userRequest.getCompanyId() == null) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId é obrigatório para o admin");
            }
            companyId = userRequest.getCompanyId();
        } else {
            companyId = requireAuthenticatedCompanyUser(request);
        }

        User user = userService.createUser(companyId, userRequest.getName(), userRequest.getEmail(), userRequest.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping
    public ResponseEntity<List<User>> listar(
            HttpServletRequest request,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Long companyId
    ) {
        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            return ResponseEntity.ok(userService.listUsersAsAdmin(nome, companyId));
        }

        Long authenticatedCompanyId = requireAuthenticatedCompanyUser(request);
        return ResponseEntity.ok(userService.listUsers(authenticatedCompanyId, nome));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<User> desativar(@PathVariable Long id, HttpServletRequest request) {
        User user;

        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            user = userService.desativarPorId(id);
        } else {
            Long companyId = requireAuthenticatedCompanyUser(request);
            Long actorUserId = requireAuthenticatedUserId(request);
            if (actorUserId.equals(id)) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode desativar o seu próprio usuário");
            }
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
            Long companyId = requireAuthenticatedCompanyUser(request);
            user = userService.ativarPorIdECompany(id, companyId);
        }

        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest userRequest,
            HttpServletRequest request
    ) {
        if (Boolean.TRUE.equals(request.getAttribute(MasterApiKey.MASTER_ACCESS_ATTRIBUTE))) {
            return ResponseEntity.ok(userService.updateUserAsAdmin(id, userRequest));
        }

        Long companyId = requireAuthenticatedCompanyUser(request);
        return ResponseEntity.ok(userService.updateUser(id, companyId, userRequest));
    }

    private Long requireAuthenticatedCompanyUser(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        Long userId = requireAuthenticatedUserId(request);
        userService.requireActiveUserInCompany(userId, companyId);
        return companyId;
    }

    private Long requireAuthenticatedUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader(USER_ID_HEADER);

        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "X-USER-ID é obrigatório para o CRUD interno de usuários");
        }

        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException exception) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "X-USER-ID inválido");
        }
    }
}
