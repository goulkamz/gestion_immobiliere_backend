package com.immobilier.gestionImmobiliere.modules.medias.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadMediaDTO {
    @NotNull private TypeEntiteMedia entiteType;
    @NotNull private Integer entiteId;
    @NotNull private MultipartFile fichier;
    private Boolean isPrincipal;
}