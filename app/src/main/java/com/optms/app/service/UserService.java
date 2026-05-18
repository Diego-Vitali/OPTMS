package com.optms.app.service;

import com.optms.app.dto.UserUpdateRequest;
import com.optms.app.model.User;
import com.optms.app.repository.UserRepository;
import java.sql.Timestamp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(Long companyId, String name, String email, String rawPassword) {
        if (name == null || name.isBlank() || email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome, e-mail e senha são obrigatórios");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com este e-mail");
        }

        User user = new User();
        user.setName(name.trim());
        user.setCompanyId(companyId);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        user.setActive(true);
        return userRepository.save(user);
    }

    public List<User> listUsers(Long companyId, String nome) {
        if (nome != null && !nome.isBlank()) {
            return userRepository.findByCompanyIdAndNameContainingIgnoreCase(companyId, nome);
        }
        return userRepository.findByCompanyId(companyId);
    }

    public List<User> listUsersAsAdmin(String nome, Long companyId) {
        if (companyId != null) {
            return listUsers(companyId, nome);
        }
        if (nome != null && !nome.isBlank()) {
            return userRepository.findByNameContainingIgnoreCase(nome);
        }
        return userRepository.findAll();
    }

    public User requireActiveUserInCompany(Long userId, Long companyId) {
        return userRepository.findByIdAndCompanyIdAndActiveTrue(userId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário autenticado inválido ou inativo"));
    }

    @Transactional
    public User updateUser(Long id, Long companyId, UserUpdateRequest request) {
        User user = userRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        return applyUpdate(user, request, companyId, false);
    }

    @Transactional
    public User updateUserAsAdmin(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        Long targetCompanyId = request.getCompanyId() != null ? request.getCompanyId() : user.getCompanyId();
        return applyUpdate(user, request, targetCompanyId, true);
    }

    private User applyUpdate(User user, UserUpdateRequest request, Long targetCompanyId, boolean allowCompanyChange) {
        if (request.getName() == null || request.getName().isBlank() || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome e e-mail são obrigatórios");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe outro usuário com este e-mail");
        }

        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        if (allowCompanyChange) {
            user.setCompanyId(targetCompanyId);
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return userRepository.save(user);
    }

    @Transactional
    public User desativarPorId(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        user.setActive(false);
        return userRepository.save(user);
    }

    @Transactional
    public User desativarPorIdECompany(Long id, Long companyId) {
        User user = userRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        user.setActive(false);
        return userRepository.save(user);
    }

    @Transactional
    public User ativarPorId(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        user.setActive(true);
        return userRepository.save(user);
    }

    @Transactional
    public User ativarPorIdECompany(Long id, Long companyId) {
        User user = userRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        user.setActive(true);
        return userRepository.save(user);
    }

    @Transactional
    public void excluirPorId(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        userRepository.delete(user);
    }

    @Transactional
    public void excluirPorIdECompany(Long id, Long companyId) {
        User user = userRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        userRepository.delete(user);
    }
}
