package com.immobilier.gestionImmobiliere.modules.biens.dto.responses;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ModifierDureeLocationResponseDTO {
    private LocationBienServiceResponseDTO location;
    private Double totalEncaisse;
    private Double totalRembourse;
    private Double solde;
}