package com.immobilier.gestionImmobiliere.modules.contrats.dto.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResilierLocationDTO {
    @NotNull private String etatDesLieuxSortie;
    private LocalDateTime dateSortie;
    @NotNull @PositiveOrZero private java.math.BigDecimal coutReparation;
}