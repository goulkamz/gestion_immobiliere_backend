package com.immobilier.gestionImmobiliere.donnees.biens.repository;

import com.immobilier.gestionImmobiliere.donnees.biens.model.LocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LocationBienServiceRepository extends JpaRepository<LocationBienService,Integer> {
    Page<LocationBienService> findByClient_IdUser(Integer idUser, Pageable pageable);

    // Locations par statut (admin)
    @Query("SELECT l.statut, COUNT(l) FROM LocationBienService l WHERE l.isDeleted = false GROUP BY l.statut")
    List<Object[]> countByStatut();

    // Locations réalisées (ACTIF/TERMINE) — compteur public "confiance"
    long countByIsDeletedFalseAndStatutIn(List<StatutLocationBienService> statuts);
}
