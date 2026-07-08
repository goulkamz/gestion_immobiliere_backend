package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutOffre;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatutOffreDTO {
    @NotNull private StatutOffre statut;
}