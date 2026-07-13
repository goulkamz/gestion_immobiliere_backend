package com.immobilier.gestionImmobiliere.modules.reservations.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.reservations.model.StatutReservation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class ReservationResponseDTO {
    private Integer idReservation;
    private Integer idUser;
    private String nomUser;
    private Integer idMaison;
    private String nomCommunMaison;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private StatutReservation statut;
}