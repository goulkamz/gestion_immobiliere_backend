package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class BailleurTopDTO {
    private Integer idBailleur;
    private String nomComplet;
    private Long nbMaisons;
}