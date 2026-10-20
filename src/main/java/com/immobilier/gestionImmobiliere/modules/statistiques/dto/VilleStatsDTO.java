package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Activité et valeur d'une ville (rattachement par secteur : maisons, cours sous mandat, biens et services). */
@Data @Builder
public class VilleStatsDTO {
    private String nomVille;
    private Long nbMaisons;
    private Long nbLocatairesActifs;
    private BigDecimal loyersEncaisses;      // loyers payés sur les maisons de la ville (période)
    private BigDecimal biensServicesNet;     // biens et services encaissés moins remboursés (période)
    private BigDecimal commissionsAcquises;  // commissions des reversements payés sur les cours de la ville (période)
    private BigDecimal volumeEncaisse;       // loyersEncaisses + biensServicesNet
    private BigDecimal revenuAgence;         // commissionsAcquises + biensServicesNet
    private BigDecimal impayesEchus;         // loyers échus non payés à ce jour (stock, hors période)
}
