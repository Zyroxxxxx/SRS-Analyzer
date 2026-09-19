package com.vlu.srsanalyzer.repository;

import com.vlu.srsanalyzer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByRole(com.vlu.srsanalyzer.entity.Role role);
    long countByRootAdminTrue();
    Optional<User> findFirstByRoleOrderByIdAsc(com.vlu.srsanalyzer.entity.Role role);
}
