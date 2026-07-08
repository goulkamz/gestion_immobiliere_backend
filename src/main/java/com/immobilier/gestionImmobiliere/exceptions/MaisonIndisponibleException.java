package com.immobilier.gestionImmobiliere.exceptions;

public class MaisonIndisponibleException extends RuntimeException {
    public MaisonIndisponibleException(Integer idMaison) {
        super("La maison id " + idMaison + " n'est pas disponible pour une location");
    }
}