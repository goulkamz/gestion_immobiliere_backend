package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Mandat actif dont la date de fin est dépassée et qui porte encore des dépenses en cours de retenue. */
@Data @Builder
public class MandatEchuDepensesDTO {
    private Integer idMandat;
    private String referenceCour;
    private LocalDateTime dateFin;
    private Long nombreDepenses;
    private BigDecimal resteARetenir;
}
