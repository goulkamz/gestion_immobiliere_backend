/*==============================================================*/
/* Migration Flyway V7 : duree de confiance d'un appareil (2FA)  */
/* Modifiable par l'admin (page Parametres) ; remplace la        */
/* variable d'environnement TWOFA_TRUST_HOURS. Sans effet si la  */
/* cle existe deja.                                              */
/*==============================================================*/

INSERT INTO parametre_systeme (cle, valeur, type_valeur, description) VALUES
    ('DUREE_CONFIANCE_APPAREIL_2FA_HEURES', '12', 'ENTIER', 'Duree pendant laquelle un appareil reconnu est dispense du code de double authentification')
ON CONFLICT (cle) DO NOTHING;
