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
    List<EcheanceLoyer> findByStatutAndDateEcheanceBefore(StatutEcheance statut, LocalDate date);

    @Query("SELECT e FROM EcheanceLoyer e WHERE " +
            "(e.entiteEcheanceType = 'LOCATION' AND e.entiteEcheanceId IN :locationIds) OR " +
            "(e.entiteEcheanceType = 'MANDAT' AND e.entiteEcheanceId IN :mandatIds)")
    Page<EcheanceLoyer> findForBailleur(@Param("locationIds") List<Integer> locationIds,
                                        @Param("mandatIds") List<Integer> mandatIds,
                                        Pageable pageable);

    Page<EcheanceLoyer> findByEntiteEcheanceTypeAndEntiteEcheanceIdIn(TypeEcheance type, List<Integer> ids, Pageable pageable);
}