package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatutLocationBienServiceDTO {
    @NotNull private StatutLocationBienService statut;
}