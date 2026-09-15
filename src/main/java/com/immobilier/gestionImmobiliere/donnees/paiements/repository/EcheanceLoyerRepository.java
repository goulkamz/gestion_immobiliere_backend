package com.immobilier.gestionImmobiliere.donnees.paiements.repository;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.modules.statistiques.projection.SumDuPaye;
import com.immobilier.gestionImmobiliere.modules.statistiques.projection.SumRetard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface EcheanceLoyerRepository extends JpaRepository<EcheanceLoyer, Integer> {
    Page<EcheanceLoyer> findByEntiteEcheanceTypeAndEntiteEcheanceId(TypeEcheance type, Integer entiteId, Pageable pageable);
    Page<EcheanceLoyer> findByStatut(StatutEcheance statut, Pageable pageable);
    List<EcheanceLoyer> findByIdEcheanceIn(List<Integer> ids);
    List<EcheanceLoyer> findByEntiteEcheanceTypeAndStatutAndDateEcheanceBefore(TypeEcheance type,StatutEcheance statut, LocalDate date);
    List<EcheanceLoyer> findByEntiteEcheanceTypeAndEntiteEcheanceIdAndStatut(TypeEcheance type, Integer entiteId, StatutEcheance statut);
    List<EcheanceLoyer> findByEntiteEcheanceTypeAndEntiteEcheanceIdAndDateEcheanceGreaterThanEqualAndStatutNot(TypeEcheance type, Integer entiteId, LocalDate date, StatutEcheance statutExclu);

    @Query("SELECT e FROM EcheanceLoyer e WHERE " +
            "(e.entiteEcheanceType = 'LOCATION' AND e.entiteEcheanceId IN :locationIds) OR " +
            "(e.entiteEcheanceType = 'MANDAT' AND e.entiteEcheanceId IN :mandatIds)")
    Page<EcheanceLoyer> findForBailleur(@Param("locationIds") List<Integer> locationIds,
                                        @Param("mandatIds") List<Integer> mandatIds,
                                        Pageable pageable);

    Page<EcheanceLoyer> findByEntiteEcheanceTypeAndEntiteEcheanceIdIn(TypeEcheance type, List<Integer> ids, Pageable pageable);

    List<EcheanceLoyer> findByEntiteEcheanceTypeAndEntiteEcheanceIdAndDateEcheanceBetween(
            TypeEcheance type, Integer entiteId, LocalDate debut, LocalDate fin);

    @Query(value = "SELECT COALESCE(SUM(e.montant_du), 0) FROM echeance_loyer e " +
            "JOIN contra_location cl ON cl.id_contra_location = e.entite_echeance_id AND e.entite_echeance_type = 'LOCATION' " +
            "JOIN maison m ON m.id_maison = cl.id_maison " +
            "WHERE m.id_cour = :idCour AND e.is_deleted = false " +
            "AND DATE_TRUNC('month', e.date_echeance) = DATE_TRUNC('month', CAST(:periode AS date))", nativeQuery = true)
    BigDecimal sumMontantDuLocationParCourEtMois(@Param("idCour") Integer idCour, @Param("periode") LocalDate periode);

    @Query(value = "SELECT COALESCE(SUM(montant_du - montant_paye), 0) FROM echeance_loyer " +
            "WHERE entite_echeance_type = 'LOCATION' AND entite_echeance_id = :idContrat " +
            "AND statut NOT IN ('PAYE', 'ANNULE') AND is_deleted = false", nativeQuery = true)
    BigDecimal sumArrieresParContrat(@Param("idContrat") Integer idContrat);

    // Statistiques echeanceLoyerRepository

    @Query("SELECT COALESCE(SUM(e.montantDu),0) AS montantDu, COALESCE(SUM(e.montantPaye),0) AS montantPaye FROM EcheanceLoyer e WHERE e.isDeleted = false")
    SumDuPaye sumDuEtPaye();

    @Query("SELECT COALESCE(COUNT(e),0) AS nombre, COALESCE(SUM(e.montantDu - e.montantPaye),0) AS montant FROM EcheanceLoyer e " +
            "WHERE e.isDeleted = false AND e.statut = com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance.EN_RETARD")
    SumRetard sumEnRetard();

    @Query(value = "SELECT u.id_user AS idLocataire, u.nom || ' ' || u.prenom AS nomComplet, " +
            "COUNT(e.id_echeance) FILTER (WHERE e.statut = 'EN_RETARD') AS nbEnRetard, " +
            "SUM((e.montant_du + e.penalite) - e.montant_paye) AS montantDu " +
            "FROM echeance_loyer e " +
            "JOIN contra_location cl ON cl.id_contra_location = e.entite_echeance_id AND e.entite_echeance_type = 'LOCATION' " +
            "JOIN users u ON u.id_user = cl.id_user " +
            "WHERE e.date_echeance <= CURRENT_DATE " +
            "AND e.statut NOT IN ('PAYE', 'ANNULE') " +
            "AND e.is_deleted = false " +
            "GROUP BY u.id_user, u.nom, u.prenom " +
            "HAVING SUM((e.montant_du + e.penalite) - e.montant_paye) > 0 " +
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

    @Query(value = "SELECT u.id_user AS idBailleur, u.nom || ' ' || u.prenom AS nomComplet, " +
            "e.date_echeance AS periode, e.montant_du AS montantDu " +
            "FROM echeance_loyer e " +
            "JOIN contrat_mandat m ON m.id_mandat = e.entite_echeance_id " +
            "JOIN cour c ON c.id_cour = m.id_cour " +
            "JOIN users u ON u.id_user = c.id_user " +
            "WHERE e.entite_echeance_type = 'MANDAT' AND e.statut = 'EN_ATTENTE' AND e.is_deleted = false " +
            "ORDER BY e.date_echeance ASC", nativeQuery = true)
    List<Object[]> bailleursCreanciers();

    // EcheanceLoyerRepository — retard, tous types confondus, pour un agent
    @Query(value = "SELECT e.id_echeance, e.entite_echeance_type, " +
            "CASE WHEN e.entite_echeance_type = 'LOCATION' " +
            "     THEN m.nom_commun_maison || ' - ' || u.nom || ' ' || u.prenom " +
            "     ELSE 'Mandat cour ' || c.reference_cour END AS libelle, " +
            "e.montant_du, e.date_echeance " +
            "FROM echeance_loyer e " +
            "LEFT JOIN contra_location cl ON cl.id_contra_location = e.entite_echeance_id AND e.entite_echeance_type = 'LOCATION' " +
            "LEFT JOIN maison m ON m.id_maison = cl.id_maison " +
            "LEFT JOIN users u ON u.id_user = cl.id_user " +
            "LEFT JOIN contrat_mandat cm ON " +
            "    (e.entite_echeance_type = 'MANDAT' AND cm.id_mandat = e.entite_echeance_id) " +
            "    OR (e.entite_echeance_type = 'LOCATION' AND cm.id_cour = m.id_cour AND cm.statut = 'ACTIF') " +
            "LEFT JOIN cour c ON c.id_cour = cm.id_cour " +
            "WHERE e.statut = 'EN_RETARD' AND e.is_deleted = false AND cm.id_user = :idAgent " +
            "ORDER BY e.date_echeance ASC", nativeQuery = true)
    List<Object[]> echeancesEnRetardPourAgent(@Param("idAgent") Integer idAgent);

    @Query(value = "SELECT e.date_echeance, e.montant_du, e.montant_paye, e.statut " +
            "FROM echeance_loyer e " +
            "JOIN contrat_mandat m ON m.id_mandat = e.entite_echeance_id " +
            "JOIN cour c ON c.id_cour = m.id_cour " +
            "WHERE e.entite_echeance_type = 'MANDAT' AND c.id_user = :idBailleur AND e.is_deleted = false " +
            "ORDER BY e.date_echeance DESC", nativeQuery = true)
    List<Object[]> revenusPourBailleur(@Param("idBailleur") Integer idBailleur);


    @Query("SELECT e FROM EcheanceLoyer e WHERE e.entiteEcheanceType = com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance.LOCATION " +
            "AND e.entiteEcheanceId IN :idsContrats AND e.isDeleted = false ORDER BY e.dateEcheance DESC")
    List<EcheanceLoyer> findEcheancesPourLocataire(@Param("idsContrats") List<Integer> idsContrats);

}