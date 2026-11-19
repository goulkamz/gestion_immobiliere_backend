package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Enregistrement de la dépense à partir d'un devis accepté. */
@Data
public class DepenseDepuisDevisDTO {
    /** Aujourd'hui par défaut, jamais dans le futur. */
    private LocalDate dateDepense;
    /** Montant réellement payé s'il est inférieur au devis ; jamais supérieur au montant accepté. */
    @Positive private BigDecimal montant;
}
