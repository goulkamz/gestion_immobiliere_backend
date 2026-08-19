package com.immobilier.gestionImmobiliere.donnees.user.repository;

import com.immobilier.gestionImmobiliere.donnees.user.model.ERole;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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


    // Total utilisateurs actifs/inactifs
    @Query("SELECT u.flagActif, COUNT(u) FROM User u WHERE u.isDeleted = false GROUP BY u.flagActif")
    List<Object[]> countByFlagActif();

    // Répartition par rôle
    @Query("SELECT r.libelleRole, COUNT(u) FROM User u JOIN u.role r WHERE u.isDeleted = false GROUP BY r.libelleRole")
    List<Object[]> countUsersByRole();

    // Nouvelles inscriptions par jour (30 derniers jours)
    @Query(value = "SELECT created_at, COUNT(*) FROM User " +
            "WHERE is_deleted = false AND created_at >= CURRENT_DATE - INTERVAL '30 days' " +
            "GROUP BY created_at ORDER BY created_at", nativeQuery = true)
    List<Object[]> countInscriptionsParJour();

    // Nouvelles inscriptions par mois
    @Query(value = "SELECT DATE_TRUNC('month', created_at) AS mois, COUNT(*) " +
            "FROM User WHERE is_deleted = false GROUP BY mois ORDER BY mois", nativeQuery = true)
    List<Object[]> countInscriptionsParMois();

    // Connexions récentes (7 derniers jours)
    @Query(value = "SELECT id_user, email, date_last_login FROM User " +
            "WHERE is_deleted = false AND date_last_login >= CURRENT_DATE - INTERVAL '7 days' " +
            "ORDER BY date_last_login DESC", nativeQuery = true)
    List<Object[]> connexionsRecentes();
}