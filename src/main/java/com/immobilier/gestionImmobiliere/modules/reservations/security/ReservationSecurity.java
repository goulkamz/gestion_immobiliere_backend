package com.immobilier.gestionImmobiliere.modules.reservations.security;

import com.immobilier.gestionImmobiliere.donnees.reservations.repository.ReservationMaisonRepository;
import org.springframework.stereotype.Component;

@Component("reservationSecurity")
public class ReservationSecurity {

    private final ReservationMaisonRepository reservationRepository;

    public ReservationSecurity(ReservationMaisonRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public boolean isOwner(Integer idReservation, Integer userId) {
        if (idReservation == null || userId == null) return false;
        return reservationRepository.findById(idReservation)
                .map(r -> r.getUser().getIdUser().equals(userId))
                .orElse(false);
    }
}