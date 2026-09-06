package com.immobilier.gestionImmobiliere.modules.contrats.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TerminerLocationDTO {
    @NotBlank(message = "L'etat des lieux est obligatoire")
    private String etatDesLieuxSortie;
    private LocalDateTime dateSortie;
    @NotBlank(message = "Le net à rembourser est obligatoire")
    private Double netARembourser;
}