package com.immobilier.gestionImmobiliere.exceptions;

// L'email ou le telephone appartient a un compte supprime (logiquement) : on ne le recree pas, un admin doit le reactiver
public class CompteSupprimeException extends RuntimeException {

    public CompteSupprimeException(String champ) {
        super("Un compte supprimé existe déjà avec " + champ + ". Demandez à un administrateur de le réactiver.");
    }
}
