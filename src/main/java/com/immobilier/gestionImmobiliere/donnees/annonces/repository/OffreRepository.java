package com.immobilier.gestionImmobiliere.donnees.annonces.repository;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Offre;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutOffre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OffreRepository extends JpaRepository<Offre, Integer> {
    Page<Offre> findByStatut(StatutOffre statut, Pageable pageable);
}