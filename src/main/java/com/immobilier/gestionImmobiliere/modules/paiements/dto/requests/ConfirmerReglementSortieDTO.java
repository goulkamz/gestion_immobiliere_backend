package com.immobilier.gestionImmobiliere.modules.paiements.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmerReglementSortieDTO {
    @NotBlank private String modePaiement;
    private String reference;
}