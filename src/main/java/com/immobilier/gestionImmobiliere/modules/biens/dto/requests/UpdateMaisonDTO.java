package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import lombok.Data;

@Data
public class UpdateMaisonDTO {
    private String typeMaison;
    private String nomCommunMaison;
    private Integer nombrePiece;
    private Double loyer;
    private Double caution;
    private Integer nombreMoisCaution;
}