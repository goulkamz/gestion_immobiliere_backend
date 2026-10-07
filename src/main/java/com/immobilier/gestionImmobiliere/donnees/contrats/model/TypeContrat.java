package com.immobilier.gestionImmobiliere.donnees.contrats.model;

import java.util.Arrays;

public enum TypeContrat {
    HABITATION, COMMERCIAL, MEUBLE;

    /**
     * Valide une valeur reçue de l'API. Retourne null si absente (champ optionnel),
     * lève IllegalArgumentException (400) si elle n'est pas un type connu.
     */
    public static TypeContrat depuisValeur(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return valueOf(valeur.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Type de contrat invalide : '" + valeur + "'. Valeurs acceptées : " + Arrays.toString(values()));
        }
    }
}
