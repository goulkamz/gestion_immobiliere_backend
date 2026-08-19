package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutBienService;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDisponibiliteBienServiceDTO {
    @NotNull private StatutBienService disponibilite;
}