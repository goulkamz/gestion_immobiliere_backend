package com.immobilier.gestionImmobiliere.exceptions;

public class CodeAlreadyExistsException extends RuntimeException {
    public CodeAlreadyExistsException(String type, String code) {
        super("Le code '" + code + "' existe déjà pour : " + type);
    }
}