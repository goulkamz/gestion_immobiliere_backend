package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateAnnonceDTO {
    private String titre;
    private String description;
    private String typeAnnonce;
    private LocalDate dateExpiration;
    private Double prix;
    private String localisation;
}