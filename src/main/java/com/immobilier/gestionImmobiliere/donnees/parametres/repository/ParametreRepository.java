package com.immobilier.gestionImmobiliere.donnees.parametres.repository;

import com.immobilier.gestionImmobiliere.donnees.parametres.model.ParametreSysteme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParametreRepository extends JpaRepository<ParametreSysteme, Integer> {
    Optional<ParametreSysteme> findByCle(String cle);
    List<ParametreSysteme> findAllByOrderByCleAsc();
}