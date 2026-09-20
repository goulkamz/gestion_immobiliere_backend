package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class GainAgenceDTO {
    private String periodeMois;   // ex: "Octobre 2026"
    private BigDecimal totalCommissions;
    private BigDecimal totalReverseAuxBailleurs;
}