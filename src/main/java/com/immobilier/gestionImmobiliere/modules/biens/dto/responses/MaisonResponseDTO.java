package com.immobilier.gestionImmobiliere.modules.biens.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class MaisonResponseDTO {
    private Integer idMaison;
    private String typeMaison;
    private String nomCommunMaison;
    private Integer nombrePiece;
    private Double loyer;
    private Double caution;
    private Integer nombreMoisCaution;
    private StatutMaison statut;
    private Integer idCour;
    private String referenceCours;
}