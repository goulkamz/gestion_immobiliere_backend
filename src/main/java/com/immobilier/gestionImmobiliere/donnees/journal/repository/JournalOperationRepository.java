package com.immobilier.gestionImmobiliere.donnees.journal.repository;

import com.immobilier.gestionImmobiliere.donnees.journal.model.JournalOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface JournalOperationRepository extends JpaRepository<JournalOperation, Integer> {

    @Query("SELECT j FROM JournalOperation j WHERE " +
            "(:idUser IS NULL OR j.idUser = :idUser) AND " +
            "(:action IS NULL OR j.action = :action) AND " +
            "(:entite IS NULL OR j.entite = :entite) AND " +
            "(:dateDebut IS NULL OR j.dateAction >= :dateDebut) AND " +
            "(:dateFin IS NULL OR j.dateAction <= :dateFin) " +
            "ORDER BY j.dateAction DESC")
    Page<JournalOperation> search(@Param("idUser") Integer idUser,
                                  @Param("action") String action,
                                  @Param("entite") String entite,
                                  @Param("dateDebut") LocalDateTime dateDebut,
                                  @Param("dateFin") LocalDateTime dateFin,
                                  Pageable pageable);
}