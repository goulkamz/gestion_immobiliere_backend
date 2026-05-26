package com.immobilier.gestionImmobiliere.exceptions;

public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException() {
        super("Le mot de passe est invalide");
    }

    public InvalidPasswordException(String message) {
        super(message);
    }
}