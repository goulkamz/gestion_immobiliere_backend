package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder
public class BailleurMandatDTO {
    private Integer idMandat;
    private String referenceCour;
    private String statut;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal commissionPourcentage;
    private String nomAgent;
}