package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateAnnonceDTO {
    private String titre;
    private String description;
    private String typeAnnonce;
    private LocalDateTime dateExpiration;
    private BigDecimal prix;
    private String localisation;
}