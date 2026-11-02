package com.immobilier.gestionImmobiliere.modules.user.dto.requests;

import jakarta.validation.constraints.NotBlank;

/** Deuxième étape de connexion : le défi reçu après le mot de passe, plus le code à 6 chiffres (ou de secours). */
public record VerifierDeuxFacteursDTO(
        @NotBlank(message = "Le jeton de vérification est obligatoire") String challengeToken,
        @NotBlank(message = "Le code est obligatoire") String code,
        boolean faireConfiance) {
}
