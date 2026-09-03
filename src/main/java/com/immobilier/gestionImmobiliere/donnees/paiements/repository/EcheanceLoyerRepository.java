package com.immobilier.gestionImmobiliere.donnees.paiements.repository;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EcheanceLoyerRepository extends JpaRepository<EcheanceLoyer, Integer> {
    Page<EcheanceLoyer> findByEntiteEcheanceTypeAndEntiteEcheanceId(TypeEcheance type, Integer entiteId, Pageable pageable);
    Page<EcheanceLoyer> findByStatut(StatutEcheance statut, Pageable pageable);
    List<EcheanceLoyer> findByIdEcheanceIn(List<Integer> ids);
    List<EcheanceLoyer> findByEntiteEcheanceTypeAndStatutAndDateEcheanceBefore(TypeEcheance type,StatutEcheance statut, LocalDate date);
    List<EcheanceLoyer> findByEntiteEcheanceTypeAndEntiteEcheanceIdAndStatut(TypeEcheance type, Integer entiteId, StatutEcheance statut);

    @Query("SELECT e FROM EcheanceLoyer e WHERE " +
            "(e.entiteEcheanceType = 'LOCATION' AND e.entiteEcheanceId IN :locationIds) OR " +
            "(e.entiteEcheanceType = 'MANDAT' AND e.entiteEcheanceId IN :mandatIds)")
    Page<EcheanceLoyer> findForBailleur(@Param("locationIds") List<Integer> locationIds,
                                        @Param("mandatIds") List<Integer> mandatIds,
                                        Pageable pageable);

    Page<EcheanceLoyer> findByEntiteEcheanceTypeAndEntiteEcheanceIdIn(TypeEcheance type, List<Integer> ids, Pageable pageable);

    List<EcheanceLoyer> findByEntiteEcheanceTypeAndEntiteEcheanceIdAndDateEcheanceBetween(
            TypeEcheance type, Integer entiteId, LocalDate debut, LocalDate fin);

    @Query(value = "SELECT COALESCE(SUM(e.montant_paye), 0) FROM echeance_loyer e " +
            "JOIN contra_location cl ON cl.id_contra_location = e.entite_echeance_id AND e.entite_echeance_type = 'LOCATION' " +
            "JOIN maison m ON m.id_maison = cl.id_maison " +
            "WHERE m.id_cour = :idCour AND e.is_deleted = false " +
            "AND DATE_TRUNC('month', e.date_echeance) = DATE_TRUNC('month', CAST(:periode AS date))", nativeQuery = true)
    Double sumMontantPayeLocationParCourEtMois(@Param("idCour") Integer idCour, @Param("periode") LocalDate periode);

    // Statistiques echeanceLoyerRepository

    @Query("SELECT COALESCE(SUM(e.montantDu),0), COALESCE(SUM(e.montantPaye),0) FROM EcheanceLoyer e WHERE e.isDeleted = false")
    Object[] sumDuEtPaye();

    @Query("SELECT COUNT(e), COALESCE(SUM(e.montantDu - e.montantPaye),0) FROM EcheanceLoyer e " +
            "WHERE e.isDeleted = false AND e.statut = com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance.EN_RETARD")
    Object[] sumEnRetard();

    @Query(value = "SELECT u.id_user AS idLocataire, u.nom || ' ' || u.prenom AS nomComplet, " +
            "COUNT(e.id_echeance) FILTER (WHERE e.statut = 'EN_RETARD') AS nbEnRetard, " +
            "SUM(e.montant_du - e.montant_paye) AS montantDu " +
            "FROM echeance_loyer e " +
            "JOIN contra_location cl ON cl.id_contra_location = e.entite_echeance_id AND e.entite_echeance_type = 'LOCATION' " +
            "JOIN users u ON u.id_user = cl.id_user " +
            "WHERE e.statut IN ('EN_ATTENTE', 'EN_RETARD') AND e.is_deleted = false " +
            "GROUP BY u.id_user, u.nom, u.prenom " +
            "HAVING SUM(e.montant_du - e.montant_paye) > 0 " +
            "ORDER BY montantDu DESC", nativeQuery = true)
    List<Object[]> locatairesEnCreance();

    @Query(value = "SELECT COALESCE(SUM(montant_du), 0) FROM echeance_loyer " +
            "WHERE entite_echeance_type = 'MANDAT' AND is_deleted = false " +
            "AND DATE_TRUNC('month', date_echeance) = DATE_TRUNC('month', CAST(:periode AS date))", nativeQuery = true)
    Double sumMontantDuAuxBailleursDuMois(@Param("periode") LocalDate periode);

    @Query(value = "SELECT COALESCE(SUM(commission_deduite), 0) FROM echeance_loyer " +
            "WHERE entite_echeance_type = 'MANDAT' AND is_deleted = false " +
            "AND DATE_TRUNC('month', date_echeance) = DATE_TRUNC('month', CAST(:periode AS date))", nativeQuery = true)
    Double sumCommissionAgenceDuMois(@Param("periode") LocalDate periode);
}