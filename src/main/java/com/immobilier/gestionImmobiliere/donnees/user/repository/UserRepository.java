package com.immobilier.gestionImmobiliere.donnees.user.repository;

import com.immobilier.gestionImmobiliere.donnees.user.model.ERole;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByTelephone(String telephone);

    Optional<User> findByNomAndPrenom(String nom, String prenom);

    // Méthode pour chercher par email OU téléphone
    @Query("SELECT u FROM User u WHERE u.email = :login OR u.telephone = :login")
    Optional<User> findByEmailOrTelephone(@Param("login") String login);

    boolean existsByEmail(String email);

    boolean existsByTelephone(String telephone);

    @NotNull Page<User> findAll(@NotNull Pageable pageable);
    Page<User> findByRole_LibelleRole(ERole role, Pageable pageable);
    boolean existsByTelephoneAndIdUserNot(String telephone, Integer idUser);
    boolean existsByEmailAndIdUserNot(String email, Integer idUser);

    @Modifying
    @Query("UPDATE User u SET u.dateLastLogin = :now WHERE u.email = :username")
    void touchLastLogin(@Param("username") String username, @Param("now") LocalDateTime now);

    // Statistiques userRepository

    @Query("SELECT r.libelleRole, COUNT(u) FROM User u JOIN u.role r WHERE u.isDeleted = false GROUP BY r.libelleRole")
    List<Object[]> countByRole();

    @Query(value = "SELECT COUNT(*) FROM users WHERE is_deleted = false AND created_at >= DATE_TRUNC('month', CURRENT_DATE)", nativeQuery = true)
    long countInscriptionsCeMois();

    @Query(value = "SELECT COUNT(*) FROM users WHERE is_deleted = false AND date_last_login >= CURRENT_DATE - INTERVAL '7 days'", nativeQuery = true)
    long countConnexionsRecentes();
}