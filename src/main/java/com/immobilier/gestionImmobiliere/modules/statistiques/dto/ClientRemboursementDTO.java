package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class ClientRemboursementDTO {
    private Integer idRemboursement;
    private BigDecimal montant;
    private String motif;
    private LocalDateTime dateRemboursement;
}