package com.immobilier.gestionImmobiliere.modules.annonces.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutAnnonce;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class AnnonceResponseDTO {
    private Integer idAnnonce;
    private String titre;
    private String description;
    private String typeAnnonce;
    private LocalDateTime datePublication;
    private LocalDateTime dateExpiration;
    private StatutAnnonce statut;
    private Double prix;
    private String localisation;
}