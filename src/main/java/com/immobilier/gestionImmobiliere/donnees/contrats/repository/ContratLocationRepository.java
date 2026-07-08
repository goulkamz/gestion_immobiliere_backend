package com.immobilier.gestionImmobiliere.donnees.contrats.repository;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratLocationRepository extends JpaRepository<ContratLocation, Integer> {
    Page<ContratLocation> findByLocataire_IdUser(Integer idUser, Pageable pageable);
    Page<ContratLocation> findByMaison_IdMaison(Integer idMaison, Pageable pageable);
    boolean existsByMaison_IdMaisonAndStatut(Integer idMaison, StatutLocation statut);
    // Ajout dans ContratLocationRepository
    Page<ContratLocation> findByMaison_Cour_Proprietaire_IdUser(Integer idUser, Pageable pageable);
}