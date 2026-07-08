package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import lombok.Data;

@Data
public class UpdateCourDTO {
    private Long idSecteur;
    private String referenceCours;
    private String lotCours;
    private Integer numeroPorte;
}