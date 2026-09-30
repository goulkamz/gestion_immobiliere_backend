package com.immobilier.gestionImmobiliere.donnees.parametres.model;

import java.util.Set;

public final class CleParametre {
    private CleParametre() {}

    public static final String TOLERANCE_LOCATION_JOURS = "TOLERANCE_LOCATION_JOURS";
    public static final String TOLERANCE_MANDAT_JOURS = "TOLERANCE_MANDAT_JOURS";
    public static final String MEDIAS_RETENTION_JOURS = "MEDIAS_RETENTION_JOURS";
    public static final String MEDIAS_MAX_FICHIERS_PAR_LOT = "MEDIAS_MAX_FICHIERS_PAR_LOT";
    public static final String PENALITE_RETARD_MONTANT = "PENALITE_RETARD_MONTANT";
    public static final String DELAI_EXPIRATION_RESERVATION_HEURES = "DELAI_EXPIRATION_RESERVATION_HEURES";
    // les clés de pénalité seront ajoutées une fois le mode de calcul confirmé

    /**
     * Liste blanche explicite des clés exposées en lecture seule sans authentification
     * (GET /api/public/parametres) — jamais un dump complet de la table : chaque clé
     * doit être ajoutée ici délibérément après vérification qu'elle est sans risque à
     * divulguer publiquement.
     */
    public static final Set<String> CLES_PUBLIQUES = Set.of(
            TOLERANCE_LOCATION_JOURS,
            DELAI_EXPIRATION_RESERVATION_HEURES
    );
}