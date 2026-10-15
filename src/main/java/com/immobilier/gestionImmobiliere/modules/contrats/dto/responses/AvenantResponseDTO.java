package com.immobilier.gestionImmobiliere.modules.contrats.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class AvenantResponseDTO {
    private Integer idAvenant;
    /** LOCATION ou MANDAT. */
    private String typeContrat;
    private Integer idContrat;
    private Integer numeroOrdre;
    private LocalDate dateEffet;
    private String motif;
    private LocalDateTime dateCreation;
    private List<Ligne> lignes;

    @Data @Builder
    public static class Ligne {
        /** Nom technique du champ : montantLoyer, dateSortie, dateFin, commission, modeFacturation, typeMandat. */
        private String champ;
        private String ancienneValeur;
        private String nouvelleValeur;
    }
}
