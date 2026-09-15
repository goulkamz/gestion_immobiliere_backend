package com.immobilier.gestionImmobiliere.modules.parametres.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.parametres.model.TypeValeurParametre;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParametreResponseDTO {
    private Integer idParametre;
    private String cle;
    private String valeur;
    private TypeValeurParametre typeValeur;
    private String description;
}