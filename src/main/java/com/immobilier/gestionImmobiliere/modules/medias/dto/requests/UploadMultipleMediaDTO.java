package com.immobilier.gestionImmobiliere.modules.medias.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class UploadMultipleMediaDTO {
    @NotNull private TypeEntiteMedia entiteType;
    @NotNull private Integer entiteId;
    @NotEmpty private List<MultipartFile> fichiers;
    private Boolean isPrincipal; // s'applique uniquement au premier fichier du lot, si demandé
}