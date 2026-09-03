package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class LocataireCreanceDTO {
    private Integer idLocataire;
    private String nomComplet;
    private Long nbEcheancesEnRetard;
    private Double montantDu;
}