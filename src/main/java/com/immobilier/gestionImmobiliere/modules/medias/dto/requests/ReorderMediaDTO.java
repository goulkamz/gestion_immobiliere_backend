package com.immobilier.gestionImmobiliere.modules.medias.dto.requests;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReorderMediaDTO {
    /** Liste ordonnée des id_media, dans l'ordre d'affichage souhaité. */
    @NotEmpty private List<Integer> idsMediaOrdonnes;
}