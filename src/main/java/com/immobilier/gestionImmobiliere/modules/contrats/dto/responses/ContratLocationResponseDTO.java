package com.immobilier.gestionImmobiliere.modules.contrats.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutLocation;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class ContratLocationResponseDTO {
    private Integer idContratLocation;
    private Integer idLocataire;
    private String nomLocataire;
    private Integer idMaison;
    private String nomCommunMaison;
    private String referenceCour;
    private LocalDateTime dateEntree;
    private LocalDateTime dateSortie;
    private BigDecimal montantLoyer;
    private StatutLocation statut;
    // Renseignes uniquement par la reponse d'une modification qui a cree un avenant
    private Integer idAvenant;
    private Integer numeroAvenant;
}
