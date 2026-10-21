package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Flux SORTANT : reversements de l'agence aux bailleurs (échéances de type MANDAT)
 * et commissions retenues par l'agence sur ces reversements.
 */
@Data @Builder
public class ReversementsStatsDTO {
    private BigDecimal effectue;            // reversements au statut PAYE
    private BigDecimal aReverser;           // échus et non payés
    private long nombreAReverser;
    private BigDecimal aVenir;              // non échus
    private BigDecimal commissionsAcquises; // commissions des reversements payés
    private BigDecimal commissionsPrevues;  // commissions des reversements non encore payés
    private BigDecimal avanceSansLoyer;     // part des reversements PAYÉS dont les loyers du mois n'ont pas été encaissés (avance de l'agence)
    private long nombreAvances;             // nombre de reversements payés concernés
}
