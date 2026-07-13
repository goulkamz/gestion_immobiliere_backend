package com.immobilier.gestionImmobiliere.modules.paiements.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class PaiementResponseDTO {
    private Integer idPaiement;
    private LocalDateTime datePaiement;
    private Double montantPaiement;
    private String modePaiement;
    private String referencePaiement;
    private List<Integer> idEcheancesCouvertes;
}