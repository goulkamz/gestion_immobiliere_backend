package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class DepenseReglementResponseDTO {
    private Integer idReglement;
    private BigDecimal montant;
    private LocalDate dateReglement;
    private String modeReglement;
    private String reference;
    private String commentaire;
    /** Règlement saisi par erreur : il est conservé mais ne compte plus. */
    private Boolean annule;
    private String motifAnnulation;
    private LocalDateTime dateAnnulation;
}
