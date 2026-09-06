package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class GainAgenceDTO {
    private String periodeMois;   // ex: "Octobre 2026"
    private Double totalCommissions;
    private Double totalReverseAuxBailleurs;
}