package com.immobilier.gestionImmobiliere.donnees.annonces.repository;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Offre;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutOffre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OffreRepository extends JpaRepository<Offre, Integer>, JpaSpecificationExecutor<Offre> {
    Page<Offre> findByStatut(StatutOffre statut, Pageable pageable);

    // Statistiques OffreRepository

    long countByIsDeletedFalseAndStatut(StatutOffre statut);
}