package com.immobilier.gestionImmobiliere.modules.localisation.dto.responses;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class VilleResponseDTO {
    private Long idVille;
    private String codeVille;
    private String nomVille;
    private Long idPays;
    private String nomPays;
}