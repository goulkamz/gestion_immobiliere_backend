package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class ReservationEnAttenteDTO {
    private Integer idReservation;
    private String nomMaison;
    private String nomClient;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
}