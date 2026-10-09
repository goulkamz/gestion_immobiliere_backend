package com.immobilier.gestionImmobiliere.donnees.biens.repository;

import com.immobilier.gestionImmobiliere.donnees.biens.model.LocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.List;

public interface LocationBienServiceRepository extends JpaRepository<LocationBienService,Integer>, JpaSpecificationExecutor<LocationBienService> {
    Page<LocationBienService> findByClient_IdUser(Integer idUser, Pageable pageable);

    // Nombre de locations ACTIF du meme bien qui chevauchent la periode [debut, fin[ (hors la location exclue)
    @Query("SELECT COUNT(l) FROM LocationBienService l WHERE l.bienService.idBienService = :idBien " +
            "AND l.statut = :statut AND l.idLocationBienService <> :idExclue " +
            "AND l.dateDebut < :fin AND l.dateFin > :debut")
    long countChevauchements(@Param("idBien") Integer idBien, @Param("statut") StatutLocationBienService statut,
                             @Param("idExclue") Integer idExclue, @Param("debut") LocalDateTime debut,
                             @Param("fin") LocalDateTime fin);

    // Statistiques LocationBienServiceRepository

    @Query("SELECT l.statut, COUNT(l) FROM LocationBienService l WHERE l.isDeleted = false GROUP BY l.statut")
    List<Object[]> countByStatut();

    List<LocationBienService> findByStatutOrderByDateDebutAsc(StatutLocationBienService statut);

    List<LocationBienService> findByClient_IdUserOrderByDateDebutDesc(Integer idClient);

    long countByIsDeletedFalseAndStatutIn(List<StatutLocationBienService> statuts);
}
