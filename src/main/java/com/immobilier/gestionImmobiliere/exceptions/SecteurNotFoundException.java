package com.immobilier.gestionImmobiliere.exceptions;

public class SecteurNotFoundException extends RuntimeException {
    public SecteurNotFoundException(Integer id) {
        super("Aucune ville trouvée avec l'id : " + id);
    }
}