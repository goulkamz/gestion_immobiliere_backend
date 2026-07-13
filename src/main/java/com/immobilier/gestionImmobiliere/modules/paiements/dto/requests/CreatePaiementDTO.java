package com.immobilier.gestionImmobiliere.modules.paiements.dto.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreatePaiementDTO {
    @NotNull private Double montantPaiement;
    @NotNull private String modePaiement;
    private String referencePaiement;
    private LocalDateTime datePaiement;

    @NotEmpty(message = "Au moins une échéance doit être sélectionnée")
    private List<Integer> idEcheances;
}