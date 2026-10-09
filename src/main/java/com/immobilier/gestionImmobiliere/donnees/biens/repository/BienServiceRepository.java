package com.immobilier.gestionImmobiliere.donnees.biens.repository;

import com.immobilier.gestionImmobiliere.donnees.biens.model.BienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutBienService;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.BienServiceResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BienServiceRepository extends JpaRepository<BienService,Integer>, JpaSpecificationExecutor<BienService> {
    Page<BienService> findBySecteur_IdSecteurAndCategorie_IdCategorie(Integer idSecteur, Integer idCategorie, Pageable pageable);
    Page<BienService> findBySecteur_IdSecteur(Integer idSecteur,Pageable pageable);
    Page<BienService> findByCategorie_IdCategorie(Integer idCategorie,Pageable pageable);

    // Statistiques bienServiceRepository

    @Query("SELECT c.libelle, bs.disponibilite, COUNT(bs) FROM BienService bs JOIN bs.categorie c WHERE bs.isDeleted = false GROUP BY c.libelle, bs.disponibilite")
    List<Object[]> countByCategorieEtDisponibilite();

    long countByIsDeletedFalseAndDisponibilite(StatutBienService disponibilite);

    // Nombre de biens et services non supprimes par categorie (une seule requete groupee)
    @Query("SELECT bs.categorie.idCategorie, COUNT(bs) FROM BienService bs WHERE bs.isDeleted = false AND bs.categorie.idCategorie IN :ids GROUP BY bs.categorie.idCategorie")
    List<Object[]> countParCategorie(@Param("ids") Collection<Integer> ids);

    long countByCategorie_IdCategorieAndIsDeletedFalse(Integer idCategorie);
}
