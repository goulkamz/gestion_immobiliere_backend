package com.immobilier.gestionImmobiliere.exceptions;

public class ConflitReservationException extends RuntimeException {
    public ConflitReservationException(Integer idMaison) {
        super("Conflit de dates : une réservation active existe déjà sur la maison id " + idMaison + " pour cette période");
    }
}