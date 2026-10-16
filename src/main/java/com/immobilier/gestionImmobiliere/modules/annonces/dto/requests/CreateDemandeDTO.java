package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateDemandeDTO {
    @NotBlank private String nomComplet;
    @Email private String email;
    private String telephone;
    private String typeBien;
    private String localisationSouhaite;
    private BigDecimal budgetMax;
    private String description;

    /** Ville repertoriee ; a defaut, {@code villeAutre} (nom libre, ville absente de la base). */
    private Integer idVille;
    @Size(max = 100) private String villeAutre;
}
