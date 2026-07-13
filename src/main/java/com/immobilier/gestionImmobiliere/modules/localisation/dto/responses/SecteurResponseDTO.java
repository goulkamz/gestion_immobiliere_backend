package com.immobilier.gestionImmobiliere.modules.localisation.dto.responses;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class SecteurResponseDTO {
    private Integer idSecteur;
    private String codeSecteur;
    private String nomSecteur;
    private Integer idVille;
    private String nomVille;
}