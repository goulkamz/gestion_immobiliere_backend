package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class ClientRemboursementDTO {
    private Integer idRemboursement;
    private Double montant;
    private String motif;
    private LocalDateTime dateRemboursement;
}