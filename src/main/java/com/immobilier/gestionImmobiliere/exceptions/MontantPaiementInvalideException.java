package com.immobilier.gestionImmobiliere.exceptions;

public class MontantPaiementInvalideException extends RuntimeException {
    public MontantPaiementInvalideException(Double attendu, Double recu) {
        super("Montant du paiement invalide : attendu " + attendu + ", reçu " + recu);
    }
}