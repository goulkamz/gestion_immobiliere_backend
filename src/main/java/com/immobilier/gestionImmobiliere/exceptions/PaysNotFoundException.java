package com.immobilier.gestionImmobiliere.exceptions;

public class PaysNotFoundException extends RuntimeException {
    public PaysNotFoundException(Long id) {
        super("Aucun pays trouvé avec l'id : " + id);
    }
}