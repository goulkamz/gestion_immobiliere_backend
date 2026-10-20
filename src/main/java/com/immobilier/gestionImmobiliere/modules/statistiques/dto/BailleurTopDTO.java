package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class BailleurTopDTO {
    private Integer idBailleur;
    private String nomComplet;
    private Long nbMaisons;
    private BigDecimal loyersEncaisses;   // loyers payés sur l'ensemble de ses maisons
}
