package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder
public class ClientEcheanceDTO {
    private Integer idEcheance;
    private String moisLibelle;
    private LocalDate dateEcheance;
    private BigDecimal montantDu;
    private BigDecimal montantPaye;
    private StatutEcheance statut;
}