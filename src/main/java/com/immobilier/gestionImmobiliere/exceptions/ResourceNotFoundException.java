package com.immobilier.gestionImmobiliere.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    private final String resource;
    private final String code;

    public ResourceNotFoundException(String resource, Object id) {
        super("Aucun(e) " + resource + " trouvé(e) avec l'id : " + id);
        this.resource = resource;
        this.code = resource.toUpperCase().replace(" ", "_") + "_NOT_FOUND";
    }

    public String getCode() {
        return code;
    }
}