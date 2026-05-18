package com.optms.app.repository;

import com.optms.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByCompanyId(Long companyId);

    List<User> findByNameContainingIgnoreCase(String name);

    List<User> findByCompanyIdAndNameContainingIgnoreCase(Long companyId, String name);

    List<User> findByCompanyIdAndActiveTrue(Long companyId);

    Optional<User> findByIdAndCompanyId(Long id, Long companyId);

    Optional<User> findByCompanyIdAndEmailIgnoreCaseAndActiveTrue(Long companyId, String email);

    Optional<User> findByIdAndCompanyIdAndActiveTrue(Long id, Long companyId);

    List<User> findByEmailIgnoreCaseAndActiveTrue(String email);

    boolean existsByCompanyIdAndEmailIgnoreCase(Long companyId, String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
