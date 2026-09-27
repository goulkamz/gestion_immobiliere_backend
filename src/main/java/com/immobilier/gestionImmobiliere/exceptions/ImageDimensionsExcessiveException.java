package com.immobilier.gestionImmobiliere.exceptions;

public class ImageDimensionsExcessiveException extends RuntimeException {
    public ImageDimensionsExcessiveException(int largeur, int hauteur, int maxPx) {
        super("Image trop grande (" + largeur + "x" + hauteur + " px, max " + maxPx + "px)");
    }
}
