package com.immobilier.gestionImmobiliere.modules.annonces.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutAnnonce;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data @Builder
public class AnnonceResponseDTO {
    private Integer idAnnonce;
    private String titre;
    private String description;
    private String typeAnnonce;
    private LocalDate datePublication;
    private LocalDate dateExpiration;
    private StatutAnnonce statut;
    private Double prix;
    private String localisation;
}