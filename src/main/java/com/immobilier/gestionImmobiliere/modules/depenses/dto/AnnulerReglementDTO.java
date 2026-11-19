package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Annulation d'un règlement saisi par erreur : toujours motivée. */
@Data
public class AnnulerReglementDTO {
    @NotBlank @Size(max = 254) private String motif;
}
