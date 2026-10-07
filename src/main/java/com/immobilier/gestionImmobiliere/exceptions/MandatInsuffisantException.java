package com.immobilier.gestionImmobiliere.exceptions;

public class MandatInsuffisantException extends RuntimeException {
    public MandatInsuffisantException(Integer idCour) {
        super("Aucun mandat actif de type GESTION ou LOCATION pour la cour id : " + idCour
                + ". Créez ou activez un mandat avant de convertir la réservation.");
    }
}
