package com.immobilier.gestionImmobiliere.donnees.user.repository;

import com.immobilier.gestionImmobiliere.donnees.user.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiration < :now")
    void deleteAllExpired(@Param("now") Instant now);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO password_reset_token (email, token, creation, expiration, attempt_count, used) " +
            "VALUES (:#{#token.email}, :#{#token.token}, :#{#token.creation}, :#{#token.expiration}, :#{#token.attemptCount}, :#{#token.used}) " +
            "ON CONFLICT (email) DO UPDATE SET " +
            "token = EXCLUDED.token, " +
            "creation = EXCLUDED.creation, " +
            "expiration = EXCLUDED.expiration, " +
            "attempt_count = EXCLUDED.attempt_count, " +
            "used = EXCLUDED.used",
            nativeQuery = true)
    void upsertToken(@Param("token") PasswordResetToken token);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.email = :email")
    void deleteByEmail(@Param("email") String email);
}