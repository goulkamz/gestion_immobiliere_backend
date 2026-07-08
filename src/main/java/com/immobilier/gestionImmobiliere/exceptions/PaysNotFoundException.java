package com.immobilier.gestionImmobiliere.exceptions;

public class PaysNotFoundException extends RuntimeException {
    public PaysNotFoundException(Long id) {
        super("Aucun localisation trouvé avec l'id : " + id);
    }
}