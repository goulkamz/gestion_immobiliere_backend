package com.immobilier.gestionImmobiliere.donnees.paiements.repository;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.PaiementLocationBienService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaiementLocationBienServiceRepository extends JpaRepository<PaiementLocationBienService, PaiementLocationBienService.PaiementLocationId> {

    List<PaiementLocationBienService> findByIdLocationBienService(Integer idLocation);

    // Calcule directement la somme via jointure SQL, sans dépendre du cache d'entités
    @Query("SELECT COALESCE(SUM(p.montantPaiement), 0) FROM PaiementLocationBienService pl " +
            "JOIN pl.paiement p WHERE pl.idLocationBienService = :idLocation")
    Double sumMontantByLocation(@Param("idLocation") Integer idLocation);

    //Statistiques PaiementLocationBienServiceRepository

    @Query("SELECT COALESCE(SUM(p.montantPaiement),0) FROM PaiementLocationBienService pl JOIN pl.paiement p")
    Double sumTotalEncaisse();
}