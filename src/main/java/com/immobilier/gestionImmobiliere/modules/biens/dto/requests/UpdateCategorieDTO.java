package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import lombok.Data;

@Data
public class UpdateCategorieDTO {
    private String libelle;
    private String description;
}