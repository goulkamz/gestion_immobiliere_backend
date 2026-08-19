package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCategorieDTO {
    @NotBlank private String libelle;
    private String description;
}