package com.immobilier.gestionImmobiliere.modules.user.dto.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class CreateUserDTO {

        private static final String PHONE_REGEX = "^(\\+\\d{1,3}[- ]?)?\\d{9,15}$";
        private static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$";
        private static final String SEXE_REGEX = "^(?i)[MF]$";

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 254, message = "Le nom ne doit pas dépasser 254 caractères")
        private String nom;

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 254, message = "Le prénom ne doit pas dépasser 254 caractères")
        private String prenom;

        @Pattern(regexp = SEXE_REGEX, message = "Le sexe doit être 'M' ou 'F'")
        @Size(min = 1, max = 1, message = "Le sexe doit être un seul caractère")
        private String sexe;

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        @Size(max = 254, message = "L'email ne doit pas dépasser 254 caractères")
        private String email;

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 6, max = 120, message = "Le mot de passe doit contenir entre 6 et 120 caractères")
        @Pattern(regexp = PASSWORD_REGEX,
                message = "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial")
        private String password;

        @NotNull(message = "La date de naissance est obligatoire")
        @Past(message = "La date de naissance doit être dans le passé")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
        private Date dateNaissance;

        @NotBlank(message = "Le téléphone est obligatoire")
        @Pattern(regexp = PHONE_REGEX, message = "Format de téléphone invalide")
        @Size(max = 254, message = "Le téléphone ne doit pas dépasser 254 caractères")
        private String telephone;

        @Pattern(regexp = PHONE_REGEX, message = "Format de téléphone invalide")
        @Size(max = 254, message = "Le téléphone ne doit pas dépasser 254 caractères")
        private String telephone1;  // Téléphone secondaire (optionnel)

        @NotNull(message = "L'ID du rôle est obligatoire")
        private Integer idRole;  // On utilise l'ID du rôle au lieu de l'objet Role complet

        @Builder.Default
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
        private Date dateCreate = new Date();  // Date de création automatique

        // Méthodes utilitaires
        public boolean isTelephoneSecondairePresent() {
            return telephone1 != null && !telephone1.trim().isEmpty();
        }

        public String getFullName() {
            return prenom + " " + nom;
        }

}
