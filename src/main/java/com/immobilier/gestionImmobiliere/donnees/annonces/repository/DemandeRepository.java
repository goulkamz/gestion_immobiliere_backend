package com.immobilier.gestionImmobiliere.donnees.annonces.repository;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Demande;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutDemande;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DemandeRepository extends JpaRepository<Demande, Integer> {
    Page<Demande> findByStatut(StatutDemande statut, Pageable pageable);

    // Statistiques demandeRepository

    @Query("SELECT d.statut, COUNT(d) FROM Demande d WHERE d.isDeleted = false GROUP BY d.statut")
    List<Object[]> countByStatut();   // Demande
}