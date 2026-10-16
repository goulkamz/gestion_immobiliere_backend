package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOffreDTO {
    @NotBlank private String nomComplet;
    @Email private String email;
    private String telephone;
    private String typeOffre;
    private String titre;
    private String description;
    private String adresse;

    /** Ville repertoriee ; a defaut, {@code villeAutre} (nom libre, ville absente de la base). */
    private Integer idVille;
    @Size(max = 100) private String villeAutre;
}
