package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class PublicStatsDTO {
    private long maisonsDisponibles;
    private long biensDisponibles;
    private long villesCouvertes;
    private long secteursCouverts;
    private long proprietesGerees;
    private long locationsRealisees;
}