package com.immobilier.gestionImmobiliere.modules.paiements.dto.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreatePaiementDTO {
    @NotNull @DecimalMin(value = "0", inclusive = false, message = "Le montant doit être supérieur à zéro")
    private BigDecimal montantPaiement;
    @NotBlank private String modePaiement;
    private String referencePaiement;
    private LocalDateTime datePaiement;

    @NotEmpty(message = "Au moins une échéance doit être sélectionnée")
    private List<Integer> idEcheances;
}