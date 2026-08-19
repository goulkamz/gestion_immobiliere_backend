package com.immobilier.gestionImmobiliere.modules.biens.dto.responses;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ModifierDureeLocationResponseDTO {
    private LocationBienServiceResponseDTO location;
    private Double montantComplementEnregistre; // renseigné uniquement en cas de prolongation payée
    private Double tropPercu;                    // renseigné uniquement en cas de raccourcissement avec écart positif
}