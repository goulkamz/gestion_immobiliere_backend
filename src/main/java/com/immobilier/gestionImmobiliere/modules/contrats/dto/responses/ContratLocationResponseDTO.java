package com.immobilier.gestionImmobiliere.modules.contrats.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutLocation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class ContratLocationResponseDTO {
    private Integer idContratLocation;
    private Integer idLocataire;
    private String nomLocataire;
    private Integer idMaison;
    private String nomCommunMaison;
    private LocalDateTime dateEntree;
    private LocalDateTime dateSortie;
    private Double montantLoyer;
    private StatutLocation statut;
}