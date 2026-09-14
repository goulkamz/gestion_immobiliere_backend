package com.immobilier.gestionImmobiliere.modules.medias.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class UploadMultipleMediaDTO {
    @NotNull private TypeEntiteMedia entiteType;
    @NotNull private Integer entiteId;
    @NotEmpty(message = "Au moins un fichier est requis")
    @Size(max = 10, message = "Maximum 10 fichiers par envoi")
    private List<MultipartFile> fichiers;
    private Boolean isPrincipal; // s'applique uniquement au premier fichier du lot, si demandé
}