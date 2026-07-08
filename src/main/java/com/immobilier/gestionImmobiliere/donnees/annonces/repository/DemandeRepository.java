package com.immobilier.gestionImmobiliere.donnees.annonces.repository;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Demande;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutDemande;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandeRepository extends JpaRepository<Demande, Integer> {
    Page<Demande> findByStatut(StatutDemande statut, Pageable pageable);
}