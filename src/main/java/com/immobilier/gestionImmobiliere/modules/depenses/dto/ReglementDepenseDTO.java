package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Règlement direct du bailleur pour une dépense due après la résiliation du mandat. */
@Data
public class ReglementDepenseDTO {
    @NotNull @Positive private BigDecimal montant;
    /** Aujourd'hui par défaut, jamais dans le futur. */
    private LocalDate dateReglement;
    @Size(max = 50) private String modeReglement;
    @Size(max = 254) private String reference;
    @Size(max = 500) private String commentaire;
}
