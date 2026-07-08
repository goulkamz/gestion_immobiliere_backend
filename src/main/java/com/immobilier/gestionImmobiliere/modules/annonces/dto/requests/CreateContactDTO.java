package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateContactDTO {
    @NotBlank private String nomComplet;
    @Email @NotBlank private String email;
    private String telephone;
    private String sujet;
    @NotBlank private String message;
}