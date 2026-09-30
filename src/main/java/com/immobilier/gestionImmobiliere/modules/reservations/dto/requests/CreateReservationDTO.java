package com.immobilier.gestionImmobiliere.modules.reservations.dto.requests;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateReservationDTO {
    @NotNull private Integer idMaison;
    // dateFin n'est pas saisie par le client : elle est calculée côté serveur
    // (dateDebut + DELAI_EXPIRATION_RESERVATION_HEURES), la réservation n'étant
    // qu'une demande de blocage temporaire, pas le bail réel (dateSortie du
    // contrat de location est fixée séparément, éventuellement plus tard, à la
    // conversion — souvent inconnue à l'avance).
    @NotNull @FutureOrPresent private LocalDateTime dateDebut;
}