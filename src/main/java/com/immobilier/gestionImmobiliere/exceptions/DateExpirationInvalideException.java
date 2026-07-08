package com.immobilier.gestionImmobiliere.exceptions;

public class DateExpirationInvalideException extends RuntimeException {
    public DateExpirationInvalideException() {
        super("La date d'expiration doit être postérieure à la date de publication");
    }
}