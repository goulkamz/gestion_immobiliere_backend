package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutAnnonce;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatutAnnonceDTO {
    @NotNull private StatutAnnonce statut;
}