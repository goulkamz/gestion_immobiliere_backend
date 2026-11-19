package com.immobilier.gestionImmobiliere.donnees.depenses.model;

/**
 * EN_ATTENTE : soumis au bailleur, qui n'a pas encore répondu.
 * A_RENEGOCIER : le bailleur demande de revoir le devis ; l'agent le modifie pour le soumettre à nouveau.
 * ACCEPTE : le bailleur est d'accord, la dépense peut être enregistrée.
 * REJETE : le bailleur refuse, les travaux se règlent hors plateforme.
 * ANNULE : retiré par l'agence.
 * REALISE : la dépense a été enregistrée à partir du devis accepté.
 * EXPIRE : resté sans réponse au-delà du délai (paramètre DELAI_REPONSE_DEVIS_JOURS) ; l'agent peut le soumettre à nouveau.
 */
public enum StatutDevis {
    EN_ATTENTE, A_RENEGOCIER, ACCEPTE, REJETE, ANNULE, REALISE, EXPIRE
}
