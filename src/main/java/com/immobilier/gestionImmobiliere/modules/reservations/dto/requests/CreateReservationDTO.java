package com.immobilier.gestionImmobiliere.modules.reservations.dto.requests;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateReservationDTO {
    @NotNull private Integer idMaison;
    @NotNull @FutureOrPresent private LocalDateTime dateDebut;
    @NotNull private LocalDateTime dateFin;
}