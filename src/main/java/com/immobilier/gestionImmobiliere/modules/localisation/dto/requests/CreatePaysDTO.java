package com.immobilier.gestionImmobiliere.modules.localisation.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePaysDTO {
    @NotBlank(message = "Le code localisation est obligatoire")
    private String codePays;

    @NotBlank(message = "Le nom du localisation est obligatoire")
    private String nomPays;
}