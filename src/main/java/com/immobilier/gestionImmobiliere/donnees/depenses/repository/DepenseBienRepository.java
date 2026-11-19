package com.immobilier.gestionImmobiliere.donnees.depenses.repository;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseBien;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDepense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DepenseBienRepository extends JpaRepository<DepenseBien, Integer>, JpaSpecificationExecutor<DepenseBien> {

    /** Dépenses d'un mandat dans un statut donné, de la plus ancienne à la plus récente (ordre de retenue). */
    List<DepenseBien> findByMandat_IdMandatAndStatutOrderByDateDepenseAscIdDepenseAsc(Integer idMandat, StatutDepense statut);

    /** Même ordre de retenue, pour plusieurs statuts (en cours et dues après résiliation). */
    List<DepenseBien> findByMandat_IdMandatAndStatutInOrderByDateDepenseAscIdDepenseAsc(Integer idMandat, List<StatutDepense> statuts);

    /** Dépense enregistrée à partir d'un devis accepté. */
    Optional<DepenseBien> findFirstByDevis_IdDevis(Integer idDevis);

    /** Dépenses engagées (hors annulées) dont la date tombe dans [debut, fin]. */
    @Query("SELECT COUNT(d) FROM DepenseBien d WHERE d.statut <> com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDepense.ANNULEE AND d.dateDepense BETWEEN :debut AND :fin")
    long countEngageesEntre(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT COALESCE(SUM(d.montant), 0) FROM DepenseBien d WHERE d.statut <> com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDepense.ANNULEE AND d.dateDepense BETWEEN :debut AND :fin")
    BigDecimal sumEngageesEntre(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    /** Ce que l'agence a avancé et pas encore retenu sur les reversements (dépenses en cours). */
    @Query("SELECT COALESCE(SUM(d.montant - d.montantImpute - d.montantRegle - d.montantAbandonne), 0) FROM DepenseBien d WHERE d.statut = com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDepense.EN_COURS")
    BigDecimal sumResteARetenir();

    /** Ce que les bailleurs doivent encore régler directement à l'agence (mandats résiliés avec un solde). */
    @Query("SELECT COALESCE(SUM(d.montant - d.montantImpute - d.montantRegle - d.montantAbandonne), 0) FROM DepenseBien d WHERE d.statut = com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDepense.A_REGLER")
    BigDecimal sumDuParLesBailleurs();

    /** Créances abandonnées par l'agence (abandon décidé entre {@code debut} et {@code fin}, bornes incluses). */
    @Query("SELECT COALESCE(SUM(d.montantAbandonne), 0) FROM DepenseBien d WHERE d.montantAbandonne > 0 AND d.dateAbandon >= :debut AND d.dateAbandon < :fin")
    BigDecimal sumAbandonneesEntre(@Param("debut") java.time.LocalDateTime debut, @Param("fin") java.time.LocalDateTime fin);

    long countByStatut(StatutDepense statut);

    /** Solde restant des dépenses d'un bailleur dans un statut donné (ses cours uniquement). */
    @Query("SELECT COALESCE(SUM(d.montant - d.montantImpute - d.montantRegle - d.montantAbandonne), 0) FROM DepenseBien d WHERE d.statut = :statut AND d.cour.proprietaire.idUser = :idBailleur")
    BigDecimal sumRestantPourBailleur(@Param("idBailleur") Integer idBailleur, @Param("statut") StatutDepense statut);

    /**
     * Mandats actifs dont la date de fin est dépassée et qui portent encore des dépenses en cours :
     * [idMandat, nombre de dépenses, solde restant à retenir].
     */
    @Query("""
            SELECT d.mandat.idMandat, COUNT(d), SUM(d.montant - d.montantImpute - d.montantRegle - d.montantAbandonne)
            FROM DepenseBien d
            WHERE d.statut = com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDepense.EN_COURS
              AND d.mandat.statut = com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat.ACTIF
              AND d.mandat.dateFin IS NOT NULL AND d.mandat.dateFin < :maintenant
            GROUP BY d.mandat.idMandat
            """)
    List<Object[]> mandatsEchusAvecDepensesEnCours(@Param("maintenant") java.time.LocalDateTime maintenant);

    @Query("SELECT COUNT(d) FROM DepenseBien d WHERE d.statut = :statut AND d.cour.proprietaire.idUser = :idBailleur")
    long countPourBailleur(@Param("idBailleur") Integer idBailleur, @Param("statut") StatutDepense statut);
}
