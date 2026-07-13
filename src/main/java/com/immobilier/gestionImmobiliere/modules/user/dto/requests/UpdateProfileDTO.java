package com.immobilier.gestionImmobiliere.modules.user.dto.requests;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileDTO {
    private String nom;
    private String prenom;
    private String telephone;
    private String telephone1;
    private LocalDate dateNaissance;
    // Volontairement absent : email, role -> RG "données non modifiables" (F4)
}