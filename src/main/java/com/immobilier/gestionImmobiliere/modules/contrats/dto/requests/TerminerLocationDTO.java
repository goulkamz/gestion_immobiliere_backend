package com.immobilier.gestionImmobiliere.modules.contrats.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TerminerLocationDTO {
    @NotBlank(message = "L'etat des lieux est obligatoire")
    private String etatDesLieuxSortie;
    private LocalDateTime dateSortie;
    @NotBlank(message = "Les frais sont obligatoires")
    @NotNull
    @PositiveOrZero
    private BigDecimal fraisReparation;
}