package com.immobilier.gestionImmobiliere.exceptions;

import org.springframework.http.HttpStatus;

/** Erreur du parcours de double authentification, avec son code HTTP et son code applicatif. */
public class DeuxFacteursException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public DeuxFacteursException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
