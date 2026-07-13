package com.immobilier.gestionImmobiliere.modules.user.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class UserAdminResponseDTO {
    private Integer idUser;
    private String email;
    private String nom;
    private String prenom;
    private Character sexe;
    private String telephone;
    private String telephone1;
    private LocalDate dateNaissance;
    private Boolean flagActif;
    private String role;
    private LocalDateTime dateCreate;
    private LocalDateTime dateLastLogin;
}