package com.immobilier.gestionImmobiliere.exceptions;

public class InvalidEmailException extends RuntimeException {

    public InvalidEmailException() {
        super("Format d'email invalide");
    }

    public InvalidEmailException(String email) {
        super("L'email '" + email + "' n'est pas valide");
    }

    public InvalidEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}