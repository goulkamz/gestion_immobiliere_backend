package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data @Builder
public class ClientEcheanceDTO {
    private Integer idEcheance;
    private String moisLibelle;
    private LocalDate dateEcheance;
    private Double montantDu;
    private Double montantPaye;
    private StatutEcheance statut;
}