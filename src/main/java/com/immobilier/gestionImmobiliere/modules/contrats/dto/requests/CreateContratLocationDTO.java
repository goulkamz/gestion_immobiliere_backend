package com.immobilier.gestionImmobiliere.modules.contrats.dto.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateContratLocationDTO {
    @NotNull private Integer idLocataire;
    @NotNull private Integer idMaison;
    @NotNull private LocalDateTime dateEntree;
    private LocalDateTime dateSortie;
    @NotNull private BigDecimal montantLoyer;
    private String typeContrat;
    private String etatDesLieuxEntree;
}