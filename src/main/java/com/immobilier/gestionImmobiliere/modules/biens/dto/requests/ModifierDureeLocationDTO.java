package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ModifierDureeLocationDTO {
    @NotNull private LocalDateTime nouvelleDateFin;

    // Renseigné uniquement si la prolongation génère un paiement complémentaire
    private Double montantComplement;
    private String modePaiementComplement;
    private String referencePaiementComplement;
}