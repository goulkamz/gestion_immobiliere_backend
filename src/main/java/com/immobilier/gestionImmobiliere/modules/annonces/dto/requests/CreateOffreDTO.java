package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

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
}