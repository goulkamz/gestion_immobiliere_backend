package com.immobilier.gestionImmobiliere.exceptions;

public class CannotDeactivateSelfException extends RuntimeException {
    public CannotDeactivateSelfException() {
        super("Un administrateur ne peut pas désactiver son propre compte");
    }
}