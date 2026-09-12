package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateMaisonDTO {
    @NotNull private Integer idCour;
    private String typeMaison;
    private String nomCommunMaison;
    private Integer nombrePiece;
    private BigDecimal loyer;
    private BigDecimal caution;
    private BigDecimal avance;
    private Integer nombreMoisCaution;
}