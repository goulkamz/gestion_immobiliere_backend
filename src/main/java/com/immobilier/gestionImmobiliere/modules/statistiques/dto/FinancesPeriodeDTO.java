package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Flux financiers d'une période (paiements datés dans la période). Sans bornes : depuis le début.
 * Les biens et services sont isolés : leurs revenus appartiennent à l'agence, ils ne passent pas par les bailleurs.
 */
@Data @Builder
public class FinancesPeriodeDTO {
    private LocalDate debut;   // null = depuis le début
    private LocalDate fin;     // null = sans limite

    // Gestion locative (loyers reçus, reversements aux bailleurs)
    private BigDecimal loyersEncaisses;
    private BigDecimal reversementsEffectues;
    private BigDecimal commissionsAcquises;

    // Biens et services (revenus propres de l'agence)
    private BiensServicesFinancesDTO biensServices;

    /** Loyers dont la date tombe dans la période (et déjà échus) : combien est payé, combien reste. */
    private LoyersPeriodeDTO loyersPeriode;
    /** Reversements dont la date tombe dans la période (et déjà échus) : combien est reversé, combien reste. */
    private ReversementsPeriodeDTO reversementsPeriode;

    /** Argent qui transite par l'agence : loyers encaissés + biens et services encaissés - remboursements. */
    private BigDecimal volumeEncaisse;
    /** Ce qui reste à l'agence : commissions acquises + biens et services nets. */
    private BigDecimal revenuAgence;

    @Data @Builder
    public static class LoyersPeriodeDTO {
        private BigDecimal dusPeriode;          // loyers + pénalités des échéances de la période, déjà échues
        private BigDecimal encaissePeriode;     // déjà payé sur ces échéances (quelle que soit la date du paiement)
        private BigDecimal resteAEncaisser;     // impayés de la période
        private long nombreEcheances;
        private long nombreImpayes;
        private BigDecimal tauxRecouvrement;    // en %, 0 si rien n'est échu
    }

    @Data @Builder
    public static class ReversementsPeriodeDTO {
        private BigDecimal totalPeriode;        // net dû aux bailleurs pour les reversements de la période, déjà échus
        private BigDecimal effectue;            // part au statut PAYE
        private BigDecimal aReverser;           // part non payée
        private long nombreReversements;
        private long nombreAReverser;
        private BigDecimal tauxReverse;         // en %, 0 si rien n'est échu
    }

    @Data @Builder
    public static class BiensServicesFinancesDTO {
        private BigDecimal encaisse;
        private BigDecimal rembourse;
        private BigDecimal net;
        private long nbLocations;     // locations ayant donné lieu à un paiement dans la période
        private List<CategorieFinanceDTO> parCategorie;
    }

    @Data @Builder
    public static class CategorieFinanceDTO {
        private String categorie;
        private BigDecimal encaisse;
        private BigDecimal rembourse;
        private BigDecimal net;
        private long nbLocations;
    }
}
