package com.immobilier.gestionImmobiliere.donnees.localisation.repository;

import com.immobilier.gestionImmobiliere.donnees.localisation.model.Pays;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaysRepository extends JpaRepository<Pays, Long> {
    boolean existsByCodePaysIgnoreCase(String codePays);
    Optional<Pays> findByCodePaysIgnoreCase(String codePays);
    Page<Pays> findAll(Pageable pageable);
}