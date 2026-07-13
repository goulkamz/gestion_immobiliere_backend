package com.immobilier.gestionImmobiliere.exceptions;

public class FormatMediaInvalideException extends RuntimeException {
    public FormatMediaInvalideException(String contentType) {
        super("Format non supporté : " + contentType + ". Formats acceptés : JPEG, PNG, WEBP");
    }
}