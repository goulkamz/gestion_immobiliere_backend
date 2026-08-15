package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import lombok.Data;

@Data
public class UpdateCourDTO {
    private Integer idSecteur;
    private String referenceCour;
    private String lotCour;
    private Integer numeroPorte;
}