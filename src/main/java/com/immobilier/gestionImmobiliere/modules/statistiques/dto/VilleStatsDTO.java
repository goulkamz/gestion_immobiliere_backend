package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class VilleStatsDTO {
    private String nomVille;
    private Long nbMaisons;
    private Long nbLocatairesActifs;
}