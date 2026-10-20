package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Une ligne par mois (premier jour du mois) pour les courbes d'évolution du tableau de bord admin. */
@Data @Builder
public class EvolutionMensuelleDTO {
    private LocalDate mois;
    private BigDecimal loyersAttendus;    // loyers + pénalités des échéances du mois
    private BigDecimal loyersEncaisses;   // paiements de loyer reçus dans le mois (date de paiement)
    private BigDecimal reversementsEffectues; // paiements aux bailleurs faits dans le mois
    private BigDecimal commissionsAcquises;   // commissions des reversements payés, par mois d'échéance
    private BigDecimal biensServicesNet;   // biens et services encaissés moins remboursés dans le mois (date de paiement)
    private long inscriptions;
    private long nouveauxContrats;
}
