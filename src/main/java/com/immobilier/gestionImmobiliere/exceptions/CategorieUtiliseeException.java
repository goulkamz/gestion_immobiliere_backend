package com.immobilier.gestionImmobiliere.exceptions;

public class CategorieUtiliseeException extends RuntimeException {
    public CategorieUtiliseeException(String libelle, long nbBiensServices) {
        super("La catégorie \"" + libelle + "\" est utilisée par " + nbBiensServices
                + " bien(s) ou service(s) et ne peut pas être supprimée");
    }
}
