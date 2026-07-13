package com.immobilier.gestionImmobiliere.modules.user.dto.requests;

import lombok.Data;

import java.time.LocalDate;

/**
 * Admin uniquement — modification complète hors mot de passe/rôle
 * (le rôle passe par UpdateUserRoleDTO, dédié, pour forcer une traçabilité distincte).
 */
@Data
public class UpdateUserByAdminDTO {
    private String nom;
    private String prenom;
    private Character sexe;
    private String telephone;
    private String telephone1;
    private LocalDate dateNaissance;
}