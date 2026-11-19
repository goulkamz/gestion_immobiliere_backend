package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Texte obligatoire : rétablissement d'une créance, contestation du bailleur ou réponse de l'agence. */
@Data
public class MotifDTO {
    @NotBlank @Size(max = 500) private String motif;
}
