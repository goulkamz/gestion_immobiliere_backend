package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data @Builder
public class BailleurPatrimoineDTO {
    private long nbCours;
    private long nbMaisons;
    private Map<String, Long> maisonsParStatut;
    private Double tauxOccupation;
}