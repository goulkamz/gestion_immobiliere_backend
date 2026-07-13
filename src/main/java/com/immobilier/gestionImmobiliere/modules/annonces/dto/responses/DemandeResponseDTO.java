package com.immobilier.gestionImmobiliere.modules.annonces.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutDemande;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class DemandeResponseDTO {
    private Integer idDemande;
    private String nomComplet;
    private String email;
    private String telephone;
    private String typeBien;
    private String localisationSouhaite;
    private Double budgetMax;
    private String description;
    private LocalDateTime dateDemande;
    private StatutDemande statut;
}