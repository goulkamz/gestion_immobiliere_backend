package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class LocataireCreanceDTO {
    private Integer idLocataire;
    private String nomComplet;
    private Long nbEcheancesEnRetard;
    private BigDecimal montantDu;
}
