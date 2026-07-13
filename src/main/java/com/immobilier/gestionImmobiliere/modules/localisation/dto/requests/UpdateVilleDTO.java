package com.immobilier.gestionImmobiliere.modules.localisation.dto.requests;

import lombok.Data;

@Data
public class UpdateVilleDTO {
    private Integer idPays;
    private String codeVille;
    private String nomVille;
}