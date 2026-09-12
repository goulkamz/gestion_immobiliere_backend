package com.immobilier.gestionImmobiliere.modules.paiements.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class PaiementResponseDTO {
    private Integer idPaiement;
    private LocalDateTime datePaiement;
    private BigDecimal montantPaiement;
    private String sens;
    private String modePaiement;
    private String referencePaiement;
    private List<Integer> idEcheancesCouvertes;
}