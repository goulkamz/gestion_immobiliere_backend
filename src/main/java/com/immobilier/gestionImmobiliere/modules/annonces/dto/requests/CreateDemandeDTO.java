package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDemandeDTO {
    @NotBlank private String nomComplet;
    @Email private String email;
    private String telephone;
    private String typeBien;
    private String localisationSouhaite;
    private Double budgetMax;
    private String description;
}