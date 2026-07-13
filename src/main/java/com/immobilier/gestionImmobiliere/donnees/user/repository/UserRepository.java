package com.immobilier.gestionImmobiliere.donnees.user.repository;

import com.immobilier.gestionImmobiliere.donnees.user.model.ERole;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    Page<User> findAll(Pageable pageable);
    Page<User> findByRole_LibelleRole(ERole role, Pageable pageable);
    boolean existsByTelephoneAndIdUserNot(String telephone, Integer idUser);
    boolean existsByEmailAndIdUserNot(String email, Integer idUser);
}