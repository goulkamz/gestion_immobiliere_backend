package com.immobilier.gestionImmobiliere.modules.biens.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class RemboursementResponseDTO {
    private Integer idRemboursement;
    private BigDecimal montant;
    private String modeRemboursement;
    private String reference;
    private String motif;
    private LocalDateTime dateRemboursement;
}