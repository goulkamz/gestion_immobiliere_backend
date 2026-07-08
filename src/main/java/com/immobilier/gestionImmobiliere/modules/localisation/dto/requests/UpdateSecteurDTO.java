package com.immobilier.gestionImmobiliere.modules.localisation.dto.requests;

import lombok.Data;

@Data
public class UpdateSecteurDTO {
    private Long idVille;
    private String codeSecteur;
    private String nomSecteur;
}