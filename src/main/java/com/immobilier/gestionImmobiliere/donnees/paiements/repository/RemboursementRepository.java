package com.immobilier.gestionImmobiliere.donnees.paiements.repository;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.Remboursement;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEntiteRemboursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RemboursementRepository extends JpaRepository<Remboursement, Integer> {

    List<Remboursement> findByEntiteTypeAndEntiteIdAndIsDeletedFalse(TypeEntiteRemboursement entiteType, Integer entiteId);

    @Query("SELECT COALESCE(SUM(r.paiement.montantPaiement), 0) FROM Remboursement r " +
            "WHERE r.entiteType = :type AND r.entiteId = :id AND r.isDeleted = false")
    BigDecimal sumMontantByEntite(@Param("type") TypeEntiteRemboursement type, @Param("id") Integer id);

    // Statistiques RemboursementRepository

    @Query("SELECT COALESCE(SUM(p.montantPaiement),0) FROM Remboursement r " +
            "JOIN r.paiement p " +
            "WHERE r.entiteType = com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEntiteRemboursement.LOCATION_BIEN_SERVICE " +
            "AND r.isDeleted = false")
    BigDecimal sumRembourseBienService();

    @Query("SELECT r FROM Remboursement r JOIN LocationBienService l ON l.idLocationBienService = r.entiteId " +
            "WHERE r.entiteType = com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEntiteRemboursement.LOCATION_BIEN_SERVICE " +
            "AND l.client.idUser = :idClient AND r.isDeleted = false ORDER BY r.paiement.datePaiement DESC")
    List<Remboursement> findRemboursementsPourClient(@Param("idClient") Integer idClient);
}