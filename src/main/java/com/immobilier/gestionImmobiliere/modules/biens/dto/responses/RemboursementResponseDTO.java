package com.immobilier.gestionImmobiliere.modules.biens.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class RemboursementResponseDTO {
    private Integer idRemboursement;
    private Double montant;
    private String modeRemboursement;
    private String reference;
    private String motif;
    private LocalDateTime dateRemboursement;
}