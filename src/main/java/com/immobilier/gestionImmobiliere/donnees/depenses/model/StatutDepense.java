package com.immobilier.gestionImmobiliere.donnees.depenses.model;

public enum StatutDepense {
    /** Reste à retenir sur les prochains reversements. */
    EN_COURS,
    /**
     * Mandat résilié avec un solde : le reste est dû par le bailleur. Il est retenu en totalité sur les
     * derniers reversements encore à calculer, sinon réglé directement à l'agence (règlement enregistré par l'agent).
     */
    A_REGLER,
    /** Entièrement récupérée (retenues et règlements directs). */
    SOLDEE,
    ANNULEE
}
