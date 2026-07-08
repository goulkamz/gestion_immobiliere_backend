package com.immobilier.gestionImmobiliere.modules.contrats.dto.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateContratLocationDTO {
    @NotNull private Integer idLocataire;
    @NotNull private Integer idMaison;
    @NotNull private LocalDate dateEntree;
    private LocalDate dateSortie;
    @NotNull private Double montantLoyer;
    private String typeContrat;
    private String etatDesLieuxEntree;
}