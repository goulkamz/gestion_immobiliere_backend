package com.immobilier.gestionImmobiliere.donnees.biens.repository;

import com.immobilier.gestionImmobiliere.donnees.biens.model.BienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutBienService;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.BienServiceResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BienServiceRepository extends JpaRepository<BienService,Integer> {
    Page<BienService> findBySecteur_IdSecteurAndCategorie_IdCategorie(Integer idSecteur, Integer idCategorie, Pageable pageable);
    Page<BienService> findBySecteur_IdSecteur(Integer idSecteur,Pageable pageable);
    Page<BienService> findByCategorie_IdCategorie(Integer idCategorie,Pageable pageable);

    // Répartition par catégorie et disponibilité (admin)
    @Query("SELECT c.libelle, bs.disponibilite, COUNT(bs) FROM BienService bs " +
            "JOIN bs.categorie c WHERE bs.isDeleted = false GROUP BY c.libelle, bs.disponibilite")
    List<Object[]> countByCategorieEtDisponibilite();

    // Nombre de biens disponibles (public + admin)
    long countByIsDeletedFalseAndDisponibilite(StatutBienService disponibilite);
}
