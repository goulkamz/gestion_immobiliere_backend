package com.immobilier.gestionImmobiliere.modules.medias.dto.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class UploadPhotosOffreDTO {
    @NotEmpty(message = "Au moins un fichier est requis")
    @Size(max = 10, message = "Maximum 10 fichiers par envoi")
    private List<MultipartFile> fichiers;
}
