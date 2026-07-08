package com.immobilier.gestionImmobiliere.exceptions;

public class EcheanceDejaPayeeException extends RuntimeException {
    public EcheanceDejaPayeeException(Integer idEcheance) {
        super("L'échéance id " + idEcheance + " est déjà payée ou annulée");
    }
}