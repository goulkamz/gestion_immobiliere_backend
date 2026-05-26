package com.immobilier.gestionImmobiliere.exceptions;



public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("L'email '" + email + "' est déjà utilisé");
    }

    public EmailAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}