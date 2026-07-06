package com.immobilier.gestionImmobiliere.modules.secteur.dto.responses;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class SecteurResponseDTO {
    private Long idSecteur;
    private String codeSecteur;
    private String nomSecteur;
    private Long idVille;
    private String nomVille;
}