package com.immobilier.gestionImmobiliere.modules.biens.dto.responses;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class CategorieResponseDTO {
    private Integer idCategorie;
    private String libelle;
    private String description;
}