package com.immobilier.gestionImmobiliere.modules.user.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.user.model.ERole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateUserByAdminDTO {
    @NotBlank @Email private String email;
    @NotBlank private String password;
    @NotBlank private String nom;
    @NotBlank private String prenom;
    private Character sexe;
    private String telephone;
    private String telephone1;
    private LocalDate dateNaissance;
    @NotNull private ERole role;
}