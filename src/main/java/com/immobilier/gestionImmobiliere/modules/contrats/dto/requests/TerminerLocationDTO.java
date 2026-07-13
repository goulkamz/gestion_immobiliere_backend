package com.immobilier.gestionImmobiliere.modules.contrats.dto.requests;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TerminerLocationDTO {
    private String etatDesLieuxSortie;
    private LocalDateTime dateSortie;
}