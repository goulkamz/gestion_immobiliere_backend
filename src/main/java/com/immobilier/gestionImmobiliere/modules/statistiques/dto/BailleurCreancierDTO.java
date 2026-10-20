package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Reversement échu et non payé, regroupé par bailleur. */
@Data @Builder
public class BailleurCreancierDTO {
    private Integer idBailleur;
    private String nomComplet;
    private long nbMois;                       // nombre d'échéances de reversement échues impayées
    private BigDecimal montantDu;              // ce que l'agence doit encore reverser
    private LocalDate plusAncienneEcheance;
}
