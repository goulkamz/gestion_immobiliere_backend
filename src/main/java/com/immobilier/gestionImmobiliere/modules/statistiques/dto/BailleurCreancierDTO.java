package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data @Builder
public class BailleurCreancierDTO {
    private Integer idBailleur;
    private String nomComplet;
    private LocalDate periodeMois;
    private Double montantDu;   // ce que l'agence doit encore reverser
}