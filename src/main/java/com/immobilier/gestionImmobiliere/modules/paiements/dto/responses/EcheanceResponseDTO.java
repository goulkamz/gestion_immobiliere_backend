package com.immobilier.gestionImmobiliere.modules.paiements.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder
public class EcheanceResponseDTO {
    private Integer idEcheance;
    private TypeEcheance type;
    private Integer entiteId;
    private LocalDate dateEcheance;
    private String moisLibelle;
    private BigDecimal montantDu;
    private BigDecimal montantPaye;
    private StatutEcheance statut;
}