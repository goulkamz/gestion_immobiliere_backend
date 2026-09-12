package com.immobilier.gestionImmobiliere.modules.biens.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class MaisonResponseDTO {
    private Integer idMaison;
    private String typeMaison;
    private String nomCommunMaison;
    private Integer nombrePiece;
    private BigDecimal loyer;
    private BigDecimal caution;
    private Integer nombreMoisCaution;
    private StatutMaison statut;
    private Integer idCour;
    private String referenceCour;
}