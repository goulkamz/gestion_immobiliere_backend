package com.immobilier.gestionImmobiliere.exceptions;

import java.math.BigDecimal;

public class MontantPaiementInvalideException extends RuntimeException {
    public MontantPaiementInvalideException(BigDecimal attendu, BigDecimal recu) {
        super("Montant du paiement invalide : attendu " + attendu + ", reçu " + recu);
    }
}