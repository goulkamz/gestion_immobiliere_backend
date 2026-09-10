package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data @Builder
public class BailleurRevenuDTO {
    private LocalDate periodeMois;
    private Double montantDu;       // net calculé pour ce mois
    private Double montantRecu;     // ce qui a déjà été effectivement viré
    private String statut;          // EN_ATTENTE / PAYE
}