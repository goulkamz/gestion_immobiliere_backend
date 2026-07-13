package com.immobilier.gestionImmobiliere.exceptions;

public class VilleNotFoundException extends RuntimeException {
    public VilleNotFoundException(Integer id) {
        super("Aucune ville trouvée avec l'id : " + id);
    }
}