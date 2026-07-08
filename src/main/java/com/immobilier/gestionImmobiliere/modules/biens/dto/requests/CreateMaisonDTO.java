package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMaisonDTO {
    @NotNull private Integer idCour;
    private String typeMaison;
    private String nomCommunMaison;
    private Integer nombrePiece;
    private Double loyer;
    private Double caution;
    private Integer nombreMoisCaution;
}