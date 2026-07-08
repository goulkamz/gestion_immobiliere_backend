package com.immobilier.gestionImmobiliere.donnees.contrats.repository;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContratLocationRepository extends JpaRepository<ContratLocation, Integer> {
    Page<ContratLocation> findByLocataire_IdUser(Integer idUser, Pageable pageable);
    Page<ContratLocation> findByMaison_IdMaison(Integer idMaison, Pageable pageable);
    boolean existsByMaison_IdMaisonAndStatut(Integer idMaison, StatutLocation statut);

    Page<ContratLocation> findByMaison_Cour_Proprietaire_IdUser(Integer idUser, Pageable pageable);

    @Query("SELECT cl.idContratLocation FROM ContratLocation cl WHERE cl.locataire.idUser = :idUser")
    List<Integer> findIdsByLocataire(@Param("idUser") Integer idUser);

    @Query("SELECT cl.idContratLocation FROM ContratLocation cl WHERE cl.maison.cour.proprietaire.idUser = :idUser")
    List<Integer> findIdsByProprietaire(@Param("idUser") Integer idUser);
}