package com.immobilier.gestionImmobiliere.modules.paiements.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data @Builder
public class EcheanceMandatResponseDTO {
    private Integer idEcheance;
    private Integer idMandat;
    private LocalDate periodeMois;
    private Double montantLoyersDus;   // null si non recalculé (ex: liste getEnAttente)
    private Double commissionDeduite;
    private Double montantNetAReverser;
    private StatutEcheance statut;
}