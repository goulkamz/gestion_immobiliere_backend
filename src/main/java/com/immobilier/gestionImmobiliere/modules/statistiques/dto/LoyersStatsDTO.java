package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Flux ENTRANT : loyers dus par les locataires à l'agence (échéances de type LOCATION).
 * "Échu" = date d'échéance atteinte. Les échéances futures ne comptent que dans {@code aVenir}.
 */
@Data @Builder
public class LoyersStatsDTO {
    private BigDecimal echuTotal;          // loyers + pénalités échus (hors annulés)
    private BigDecimal encaisseSurEchu;    // montant déjà payé sur ces échéances
    private BigDecimal resteAEncaisser;    // impayés échus (pénalités comprises)
    private long nombreImpayesEchus;       // même définition que la liste des créances locataires
    private BigDecimal tauxRecouvrement;   // encaisseSurEchu / echuTotal, en %
    private BigDecimal aVenir;             // échéances futures non encore exigibles
    private BigDecimal payeAvance;         // déjà payé sur ces échéances futures (loyers réglés d'avance)
    private AncienneteImpayesDTO anciennete; // répartition des impayés échus selon l'ancienneté de l'échéance

    /** Impayés échus par ancienneté (jours écoulés depuis la date d'échéance). */
    @Data @Builder
    public static class AncienneteImpayesDTO {
        private BigDecimal jours0a30;
        private BigDecimal jours31a60;
        private BigDecimal jours61a90;
        private BigDecimal plusDe90;
    }
}
