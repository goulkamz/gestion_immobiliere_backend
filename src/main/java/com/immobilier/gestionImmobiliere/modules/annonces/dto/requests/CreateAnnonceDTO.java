package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateAnnonceDTO {
    @NotBlank private String titre;
    private String description;
    private String typeAnnonce;
    @NotNull @Future(message = "La date d'expiration doit être future")
    private LocalDate dateExpiration;
    private Double prix;
    private String localisation;
}