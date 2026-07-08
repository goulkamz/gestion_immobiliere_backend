package com.immobilier.gestionImmobiliere.exceptions;

public class MandatActifExistantException extends RuntimeException {
    public MandatActifExistantException(Integer idCour) {
        super("Un mandat est déjà actif pour la cour id : " + idCour);
    }
}