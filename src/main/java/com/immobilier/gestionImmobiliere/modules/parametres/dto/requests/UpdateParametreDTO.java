package com.immobilier.gestionImmobiliere.modules.parametres.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateParametreDTO {
    @NotBlank
    private String valeur;
}