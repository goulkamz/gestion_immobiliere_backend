package com.immobilier.gestionImmobiliere.modules.annonces.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutOffre;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

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
    private LocalDateTime dateOffre;
    private StatutOffre statut;

    // Renvoye uniquement a la creation : autorise le depot de photos sur cette offre
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jetonDepotPhotos;
}