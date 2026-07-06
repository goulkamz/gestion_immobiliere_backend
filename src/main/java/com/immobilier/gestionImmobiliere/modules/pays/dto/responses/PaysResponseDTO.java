package com.immobilier.gestionImmobiliere.modules.pays.dto.responses;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class PaysResponseDTO {
    private Long idPays;
    private String codePays;
    private String nomPays;
}