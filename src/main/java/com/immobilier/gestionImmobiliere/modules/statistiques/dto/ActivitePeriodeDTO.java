package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/** Éléments créés ou reçus pendant une période (date de création), pour l'onglet Activité du tableau de bord admin. */
@Data @Builder
public class ActivitePeriodeDTO {
    private LocalDate debut;   // null = depuis le début
    private LocalDate fin;     // null = sans limite
    private long mandats;
    private long contratsLocation;
    private long locationsBienService;
    private long annonces;
    private long demandes;
}
