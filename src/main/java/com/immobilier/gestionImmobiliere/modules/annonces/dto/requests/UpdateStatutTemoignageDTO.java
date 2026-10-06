package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatutTemoignageDTO {
    @NotNull private Boolean flagActif;
}
