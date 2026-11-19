package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Abandon de créance sur le reste dû d'une dépense : décision de l'admin, toujours motivée. */
@Data
public class AbandonnerDepenseDTO {
    @NotBlank @Size(max = 254) private String motif;
}
