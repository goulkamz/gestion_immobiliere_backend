package com.immobilier.gestionImmobiliere.modules.medias.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class MediaResponseDTO {
    private Integer idMedia;
    private TypeEntiteMedia entiteType;
    private String urlThumbnail;
    private Integer entiteId;
    private String typeMedia;
    private String url;
    private Boolean isPrincipal;
    private Short ordre;
    private LocalDateTime dateUpload;
}