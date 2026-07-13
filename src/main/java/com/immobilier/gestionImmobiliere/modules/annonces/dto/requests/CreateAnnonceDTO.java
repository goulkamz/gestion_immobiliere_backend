package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateAnnonceDTO {
    @NotBlank private String titre;
    private String description;
    private String typeAnnonce;
    @NotNull @Future(message = "La date d'expiration doit être future")
    private LocalDateTime dateExpiration;
    private Double prix;
    private String localisation;
}