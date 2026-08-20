package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreerRemboursementDTO {
    @NotNull @Positive private Double montant;
    @NotBlank private String modeRemboursement;
    private String reference;
    private String motif;
}