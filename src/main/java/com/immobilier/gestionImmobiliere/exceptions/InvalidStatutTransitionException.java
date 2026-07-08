package com.immobilier.gestionImmobiliere.exceptions;

public class InvalidStatutTransitionException extends RuntimeException {
    public InvalidStatutTransitionException(String from, String to) {
        super("Transition de statut invalide : " + from + " → " + to);
    }
}