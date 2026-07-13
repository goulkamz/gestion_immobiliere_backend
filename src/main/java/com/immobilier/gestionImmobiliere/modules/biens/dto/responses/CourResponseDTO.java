package com.immobilier.gestionImmobiliere.modules.biens.dto.responses;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class CourResponseDTO {
    private Integer idCour;
    private String referenceCours;
    private String lotCours;
    private Integer numeroPorte;
    private Integer idSecteur;
    private String nomSecteur;
    private Integer idProprietaire;
    private String nomProprietaire;
}