package com.immobilier.gestionImmobiliere.donnees.user.repository;

import com.immobilier.gestionImmobiliere.donnees.user.model.PendingRegistration;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PendingRegistration p WHERE p.email = :email")
    Optional<PendingRegistration> findByEmailWithLock(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PendingRegistration p WHERE p.code = :code")
    Optional<PendingRegistration> findByCodeWithLock(@Param("code") String code);

    @Modifying
    @Transactional
    @Query("DELETE FROM PendingRegistration p WHERE p.expiration < :now")
    void deleteAllExpired(@Param("now") Instant now);

    boolean existsByEmail(String email);
}