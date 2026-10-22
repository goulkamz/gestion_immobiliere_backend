package com.immobilier.gestionImmobiliere.exceptions;

public class LocalisationUtiliseeException extends RuntimeException {
    /** @param designation article compris, ex. « la ville » */
    public LocalisationUtiliseeException(String designation, String nom, String dependances) {
        super("Impossible de supprimer " + designation + " \"" + nom + "\" : éléments encore rattachés (" + dependances + ")");
    }
}
