package com.immobilier.gestionImmobiliere.exceptions;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(String roleName) {
        super("Rôle non trouvé: " + roleName);
    }
}