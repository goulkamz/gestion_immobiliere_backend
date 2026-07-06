package com.immobilier.gestionImmobiliere.modules.pays.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePaysDTO {
    @NotBlank(message = "Le code pays est obligatoire")
    private String codePays;

    @NotBlank(message = "Le nom du pays est obligatoire")
    private String nomPays;
}