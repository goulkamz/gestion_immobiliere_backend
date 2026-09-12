package com.immobilier.gestionImmobiliere.modules.paiements.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder
public class EcheanceMandatResponseDTO {
    private Integer idEcheance;
    private Integer idMandat;
    private LocalDate periodeMois;
    private BigDecimal montantLoyersDus;   // null si non recalculé (ex: liste getEnAttente)
    private BigDecimal commissionDeduite;
    private BigDecimal montantNetAReverser;
    private StatutEcheance statut;
}