package com.immobilier.gestionImmobiliere.exceptions;

public class TailleMediaExcessiveException extends RuntimeException {
    public TailleMediaExcessiveException(long tailleOctets) {
        super("Fichier trop volumineux (" + (tailleOctets / 1024 / 1024) + " Mo) pour son type");
    }
}