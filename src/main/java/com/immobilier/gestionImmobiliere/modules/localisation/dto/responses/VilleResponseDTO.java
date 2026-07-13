package com.immobilier.gestionImmobiliere.modules.localisation.dto.responses;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class VilleResponseDTO {
    private Integer idVille;
    private String codeVille;
    private String nomVille;
    private Integer idPays;
    private String nomPays;
}