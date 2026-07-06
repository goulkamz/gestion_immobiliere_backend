package com.immobilier.gestionImmobiliere.modules.ville.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateVilleDTO {
    @NotNull(message = "Le pays est obligatoire")
    private Long idPays;

    @NotBlank(message = "Le code ville est obligatoire")
    private String codeVille;

    @NotBlank(message = "Le nom de la ville est obligatoire")
    private String nomVille;
}