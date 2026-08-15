package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCourDTO {
    @NotNull private Integer idSecteur;
    @NotNull private Integer idProprietaire;
    @NotBlank private String referenceCour;
    private String lotCour;
    private Integer numeroPorte;
}