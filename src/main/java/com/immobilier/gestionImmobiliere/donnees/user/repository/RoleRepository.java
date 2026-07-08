package com.immobilier.gestionImmobiliere.donnees.user.repository;

import com.immobilier.gestionImmobiliere.donnees.user.model.ERole;
import com.immobilier.gestionImmobiliere.donnees.user.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByLibelleRole(ERole libelleRole);
    Boolean existsByLibelleRole(ERole libelleRole);
}
