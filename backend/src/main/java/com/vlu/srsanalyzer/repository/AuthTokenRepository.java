package com.vlu.srsanalyzer.repository;

import com.vlu.srsanalyzer.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    @Query("select t from AuthToken t join fetch t.user where t.token = :token")
    Optional<AuthToken> findByTokenWithUser(@Param("token") String token);

    @Modifying
    void deleteByToken(String token);

    @Modifying
    void deleteByUserId(Long userId);
}
