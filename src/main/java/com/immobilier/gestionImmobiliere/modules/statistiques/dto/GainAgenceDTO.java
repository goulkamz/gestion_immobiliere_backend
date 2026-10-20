package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class GainAgenceDTO {
    private String periodeMois;   // ex: "Octobre 2026"
    private BigDecimal commissionsAcquises;  // sur reversements payés
    private BigDecimal commissionsPrevues;   // sur reversements pas encore payés
    private BigDecimal reverseEffectue;      // reversements payés aux bailleurs
    private BigDecimal reversePrevu;         // reversements du mois pas encore payés
}
