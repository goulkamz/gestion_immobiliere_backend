package com.immobilier.gestionImmobiliere.modules.paiements.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmerVirementDTO {
    @NotBlank private String modeVersement;
    private String reference;
}