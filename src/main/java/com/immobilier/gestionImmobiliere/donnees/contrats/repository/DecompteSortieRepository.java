package com.immobilier.gestionImmobiliere.donnees.contrats.repository;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.DecompteSortie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DecompteSortieRepository extends JpaRepository<DecompteSortie, Integer> {
    java.util.Optional<DecompteSortie> findByContratLocation_IdContratLocation(Integer idContratLocation);

    @Query("SELECT d FROM DecompteSortie d WHERE d.statut = com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutDecompteSortie.REGLE " +
            "AND FUNCTION('DATE_TRUNC', 'month', d.updatedAt) = FUNCTION('DATE_TRUNC', 'month', CAST(:periode AS date)) " +
            "AND d.isDeleted = false")
    List<DecompteSortie> findReglesDuMois(@Param("periode") LocalDate periode);
}