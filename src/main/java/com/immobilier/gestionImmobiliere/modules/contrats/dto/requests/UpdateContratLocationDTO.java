package com.immobilier.gestionImmobiliere.modules.contrats.dto.requests;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UpdateContratLocationDTO {
    // Prolonge ou réduit le bail sans le clôturer (à distinguer de terminer()/
    // resilierContratLocation() qui fixent dateSortie au moment de mettre fin au contrat)
    private LocalDateTime dateSortie;
    private BigDecimal montantLoyer;

    // Avenant : obligatoires pour modifier un contrat ACTIF (le client doit avoir accepte la modification)
    private String motif;
    private Boolean accordClient;
    /** Date d'effet de la modification ; aujourd'hui par defaut, jamais dans le passe. */
    private LocalDate dateEffet;
}
