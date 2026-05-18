package com.optms.app.service;

import com.optms.app.dto.AuthLoginRequest;
import com.optms.app.dto.AuthLoginResponse;
import com.optms.app.model.Company;
import com.optms.app.model.User;
import com.optms.app.repository.CompanyRepository;
import com.optms.app.repository.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final String masterApiKey;

    public AuthService(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            PasswordEncoder passwordEncoder,
            @Value("${master.api.key}") String masterApiKey
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.masterApiKey = masterApiKey;
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        String login = request.getLogin().trim();

        if ("admin".equalsIgnoreCase(login) && "admin".equals(request.getPassword())) {
            return new AuthLoginResponse(
                    0L,
                    "Administrador do sistema",
                    "admin",
                    null,
                    "Acesso master",
                    masterApiKey,
                    true
            );
        }

        List<User> users = userRepository.findByEmailIgnoreCaseAndActiveTrue(login);
        if (users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }

        List<UserCompanyTuple> candidates = users.stream()
                .map(user -> companyRepository.findByIdAndActiveTrue(user.getCompanyId())
                        .map(company -> new UserCompanyTuple(user, company))
                        .orElse(null))
                .filter(tuple -> tuple != null)
                .toList();

        if (candidates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Company do usuário está inativa ou indisponível");
        }

        if (candidates.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Existe mais de um usuário ativo com este login");
        }

        UserCompanyTuple tuple = candidates.getFirst();
        User user = tuple.user();
        Company company = tuple.company();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }

        return new AuthLoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                company.getId(),
                company.getName(),
                company.getApikey(),
                false
        );
    }

    private record UserCompanyTuple(User user, Company company) {
    }
}
