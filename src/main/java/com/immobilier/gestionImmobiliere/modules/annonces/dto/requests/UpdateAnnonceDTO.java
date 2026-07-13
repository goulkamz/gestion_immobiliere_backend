package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateAnnonceDTO {
    private String titre;
    private String description;
    private String typeAnnonce;
    private LocalDateTime dateExpiration;
    private Double prix;
    private String localisation;
}