package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Demande de renégociation ou rejet par le bailleur, annulation par l'agence : toujours motivé. */
@Data
public class ReponseDevisDTO {
    @NotBlank @Size(max = 254) private String commentaire;
}
