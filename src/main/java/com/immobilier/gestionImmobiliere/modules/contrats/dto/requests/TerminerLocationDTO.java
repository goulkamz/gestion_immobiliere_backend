package com.immobilier.gestionImmobiliere.modules.contrats.dto.requests;

import lombok.Data;

@Data
public class TerminerLocationDTO {
    private String etatDesLieuxSortie;
    private java.time.LocalDate dateSortie;
}