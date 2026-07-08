package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutContact;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatutContactDTO {
    @NotNull private StatutContact statut;
}