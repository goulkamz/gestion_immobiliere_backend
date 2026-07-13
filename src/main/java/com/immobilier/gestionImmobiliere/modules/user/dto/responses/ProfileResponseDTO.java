package com.immobilier.gestionImmobiliere.modules.user.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data @Builder
public class ProfileResponseDTO {
    private Integer idUser;
    private String email;
    private String nom;
    private String prenom;
    private Character sexe;
    private String telephone;
    private String telephone1;
    private LocalDate dateNaissance;
    private String role;
}