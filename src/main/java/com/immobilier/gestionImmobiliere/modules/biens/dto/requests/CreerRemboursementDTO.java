package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreerRemboursementDTO {
    @NotNull @Positive private BigDecimal montant;
    @NotBlank private String modeRemboursement;
    private String reference;
    private String motif;
}