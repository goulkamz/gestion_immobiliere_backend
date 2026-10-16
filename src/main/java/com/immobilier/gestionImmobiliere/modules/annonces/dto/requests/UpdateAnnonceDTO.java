package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import jakarta.validation.constraints.Size;
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

    /** Ville repertoriee ; a defaut, {@code villeAutre} (nom libre, ville absente de la base). */
    private Integer idVille;
    @Size(max = 100) private String villeAutre;
}
