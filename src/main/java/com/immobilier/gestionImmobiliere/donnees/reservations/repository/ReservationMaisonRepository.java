package com.immobilier.gestionImmobiliere.donnees.reservations.repository;

import com.immobilier.gestionImmobiliere.donnees.reservations.model.ReservationMaison;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationMaisonRepository extends JpaRepository<ReservationMaison, Integer> {

    Page<ReservationMaison> findByUser_IdUser(Integer idUser, Pageable pageable);
    Page<ReservationMaison> findByMaison_IdMaison(Integer idMaison, Pageable pageable);

    /**
     * RG2 — détecte tout chevauchement avec une réservation active (EN_ATTENTE ou CONFIRMEE)
     * sur la même maison. Formule standard d'intersection d'intervalles :
     * (debut1 <= fin2) AND (fin1 >= debut2)
     */
    @Query("SELECT r FROM ReservationMaison r WHERE r.maison.idMaison = :idMaison " +
            "AND r.statut IN ('EN_ATTENTE', 'CONFIRMEE') " +
            "AND r.dateDebut <= :dateFin AND r.dateFin >= :dateDebut")
    List<ReservationMaison> findConflits(@Param("idMaison") Integer idMaison,
                                         @Param("dateDebut") LocalDateTime dateDebut,
                                         @Param("dateFin") LocalDateTime dateFin);
}