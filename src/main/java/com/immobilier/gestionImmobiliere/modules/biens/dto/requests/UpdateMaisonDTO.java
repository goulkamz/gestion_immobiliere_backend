package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateMaisonDTO {
    private String typeMaison;
    private String nomCommunMaison;
    private Integer nombrePiece;
    private BigDecimal loyer;
    private BigDecimal caution;
    private Integer nombreMoisCaution;
}