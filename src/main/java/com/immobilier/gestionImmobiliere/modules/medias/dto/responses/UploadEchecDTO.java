package com.immobilier.gestionImmobiliere.modules.medias.dto.responses;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class UploadEchecDTO {
    private String nomFichier;
    private String motif;
}