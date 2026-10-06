package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutTemoignage;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatutTemoignageDTO {
    @NotNull private StatutTemoignage statut;
}
