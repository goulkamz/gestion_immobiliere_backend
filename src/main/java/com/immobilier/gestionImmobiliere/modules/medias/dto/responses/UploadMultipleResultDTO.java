package com.immobilier.gestionImmobiliere.modules.medias.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class UploadMultipleResultDTO {
    private List<MediaResponseDTO> reussis;
    private List<UploadEchecDTO> echecs;
}