package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import lombok.Data;

@Data
public class UpdateCourDTO {
    private Integer idSecteur;
    private String referenceCours;
    private String lotCours;
    private Integer numeroPorte;
}