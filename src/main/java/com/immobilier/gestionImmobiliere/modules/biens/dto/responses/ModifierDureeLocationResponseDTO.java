package com.immobilier.gestionImmobiliere.modules.biens.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class ModifierDureeLocationResponseDTO {
    private LocationBienServiceResponseDTO location;
    private BigDecimal totalEncaisse;
    private BigDecimal totalRembourse;
    private BigDecimal solde;
}