package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatutMaisonDTO {
    @NotNull private StatutMaison statut;
}