package com.optms.app.service;

import com.optms.app.model.User;
import com.optms.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
}
