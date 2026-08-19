package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConfirmerLocationDTO {
    @NotNull private Double montantPaiement;
    @NotBlank private String modePaiement;
    private String referencePaiement;
    // Optionnel : l'agent peut ajuster les dates si négociées au comptoir
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
}