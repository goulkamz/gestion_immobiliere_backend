package com.immobilier.gestionImmobiliere.exceptions;

public class PaysNotFoundException extends RuntimeException {
    public PaysNotFoundException(Integer id) {
        super("Aucun pays trouvé avec l'id : " + id);
    }
}