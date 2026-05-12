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
}
