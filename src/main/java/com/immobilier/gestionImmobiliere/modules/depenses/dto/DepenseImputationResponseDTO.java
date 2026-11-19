package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder
public class DepenseImputationResponseDTO {
    private Integer idImputation;
    private Integer idDepense;
    private String libelleDepense;
    private Integer idEcheance;
    /** Mois du reversement sur lequel la retenue a été faite. */
    private LocalDate periodeMois;
    private BigDecimal montant;
    private StatutEcheance statutReversement;
}
