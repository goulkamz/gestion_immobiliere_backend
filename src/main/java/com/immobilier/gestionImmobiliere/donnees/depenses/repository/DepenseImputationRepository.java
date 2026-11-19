package com.immobilier.gestionImmobiliere.donnees.depenses.repository;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseImputation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DepenseImputationRepository extends JpaRepository<DepenseImputation, Integer> {

    List<DepenseImputation> findByDepense_IdDepenseOrderByEcheance_DateEcheanceAsc(Integer idDepense);

    List<DepenseImputation> findByEcheance_IdEcheance(Integer idEcheance);

    /** Retenue déjà faite pour cette dépense sur ce reversement (complétée à la résiliation plutôt que doublée). */
    java.util.Optional<DepenseImputation> findFirstByDepense_IdDepenseAndEcheance_IdEcheance(Integer idDepense, Integer idEcheance);

    /** Total retenu sur les reversements dont l'échéance tombe dans [debut, fin]. */
    @Query("SELECT COALESCE(SUM(i.montant), 0) FROM DepenseImputation i WHERE i.echeance.dateEcheance BETWEEN :debut AND :fin")
    BigDecimal sumRetenuesEntre(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);
}
