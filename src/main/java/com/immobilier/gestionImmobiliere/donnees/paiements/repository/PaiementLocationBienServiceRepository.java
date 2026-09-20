package com.immobilier.gestionImmobiliere.donnees.paiements.repository;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.PaiementLocationBienService;
import com.immobilier.gestionImmobiliere.modules.statistiques.projection.SumEncaisse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PaiementLocationBienServiceRepository extends JpaRepository<PaiementLocationBienService, PaiementLocationBienService.PaiementLocationId> {

    List<PaiementLocationBienService> findByIdLocationBienService(Integer idLocation);

    // Calcule directement la somme via jointure SQL, sans dépendre du cache d'entités
    @Query("SELECT COALESCE(SUM(p.montantPaiement), 0) FROM PaiementLocationBienService pl " +
            "JOIN pl.paiement p WHERE pl.idLocationBienService = :idLocation")
    BigDecimal sumMontantByLocation(@Param("idLocation") Integer idLocation);

    List<PaiementLocationBienService> findByIdPaiement(Integer idPaiement);

    //Statistiques PaiementLocationBienServiceRepository

    @Query("SELECT COALESCE(SUM(p.montantPaiement),0) AS total FROM PaiementLocationBienService pl JOIN pl.paiement p")
    SumEncaisse sumTotalEncaisse();

    @Query(value = "SELECT COALESCE(SUM(p.montant_paiement), 0) FROM paiement_location_bien_service pl " +
            "JOIN paiement p ON p.id_paiement = pl.id_paiement " +
            "WHERE p.sens = 'ENTREE' AND DATE_TRUNC('month', p.date_paiement) = DATE_TRUNC('month', CAST(:periode AS date))",
            nativeQuery = true)
    BigDecimal sumEncaisseBienServiceDuMois(@Param("periode") LocalDate periode);
}