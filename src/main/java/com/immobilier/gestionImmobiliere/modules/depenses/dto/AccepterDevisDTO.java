package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/** Le bailleur renvoie le montant et l'étalement qu'il a sous les yeux : l'accord ne vaut que pour eux. */
@Data
public class AccepterDevisDTO {
    @NotNull @Positive private BigDecimal montant;
    @NotNull private Integer nombreMois;
}
