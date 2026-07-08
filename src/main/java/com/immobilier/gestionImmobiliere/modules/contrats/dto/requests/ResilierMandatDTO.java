package com.immobilier.gestionImmobiliere.modules.contrats.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResilierMandatDTO {
    @NotBlank(message = "Le motif de résiliation est obligatoire")
    private String motifResiliation;
}