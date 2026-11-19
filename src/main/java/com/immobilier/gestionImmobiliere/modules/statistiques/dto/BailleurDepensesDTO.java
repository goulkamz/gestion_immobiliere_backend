package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Tableau de bord du bailleur : dépenses avancées par l'agence sur ses cours et devis qui attendent sa réponse. */
@Data @Builder
public class BailleurDepensesDTO {
    /** Solde des dépenses en cours, qui sera retenu sur les prochains reversements. */
    private BigDecimal resteARetenir;
    private long nombreEnCours;
    /** Solde dû directement à l'agence (mandat résilié). */
    private BigDecimal duDirectement;
    private long nombreDues;
    private long devisEnAttente;
}
