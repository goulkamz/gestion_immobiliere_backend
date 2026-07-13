package com.immobilier.gestionImmobiliere.modules.localisation.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSecteurDTO {
    @NotNull(message = "La ville est obligatoire")
    private Integer idVille;

    @NotBlank(message = "Le code secteur est obligatoire")
    private String codeSecteur;

    @NotBlank(message = "Le nom du secteur est obligatoire")
    private String nomSecteur;
}