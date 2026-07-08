package com.immobilier.gestionImmobiliere.modules.annonces.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutOffre;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data @Builder
public class OffreResponseDTO {
    private Integer idOffre;
    private String nomComplet;
    private String email;
    private String telephone;
    private String typeOffre;
    private String titre;
    private String description;
    private String adresse;
    private LocalDate dateOffre;
    private StatutOffre statut;
}