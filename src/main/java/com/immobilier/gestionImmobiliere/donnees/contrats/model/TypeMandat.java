package com.immobilier.gestionImmobiliere.donnees.contrats.model;

import java.util.List;

public enum TypeMandat {
    GESTION(ModeFacturation.MENSUEL, ModeFacturation.CUMUL),
    // Pas encore disponibles : la location et la vente sont traitées hors système via les annonces
    // (aucun mode, refusées à la création et à la modification). Les mandats LOCATION existants restent lisibles.
    LOCATION(),
    VENTE();

    private final List<ModeFacturation> modesAutorises;

    TypeMandat(ModeFacturation... modesAutorises) {
        this.modesAutorises = List.of(modesAutorises);
    }

    public List<ModeFacturation> getModesAutorises() {
        return modesAutorises;
    }

    public boolean isDisponible() {
        return !modesAutorises.isEmpty();
    }

    public boolean accepte(ModeFacturation mode) {
        return modesAutorises.contains(mode);
    }
}
