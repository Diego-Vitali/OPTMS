package com.optms.app.controller.api;

import com.optms.app.dto.UserRequest;
import com.optms.app.dto.UserUpdateRequest;
import com.optms.app.model.User;
import com.optms.app.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Long companyId
    ) {
        return ResponseEntity.ok(userService.listUsersAsAdmin(nome, companyId));
    }

    @PostMapping
    public ResponseEntity<User> criar(@Valid @RequestBody UserRequest request) {
        if (request.getCompanyId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId é obrigatório");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(request.getCompanyId(), request.getName(), request.getEmail(), request.getPassword()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> atualizar(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUserAsAdmin(id, request));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<User> ativar(@PathVariable Long id) {
        return ResponseEntity.ok(userService.ativarPorId(id));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<User> desativar(@PathVariable Long id) {
        return ResponseEntity.ok(userService.desativarPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        userService.excluirPorId(id);
        return ResponseEntity.noContent().build();
    }
}
