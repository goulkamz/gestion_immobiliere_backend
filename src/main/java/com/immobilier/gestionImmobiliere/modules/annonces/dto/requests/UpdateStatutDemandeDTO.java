package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutDemande;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatutDemandeDTO {
    @NotNull private StatutDemande statut;
}