/*==============================================================*/
/* Migration Flyway V3 : parametres systeme par defaut          */
/* Donnees de reference (et non de demonstration) : necessaires */
/* en production pour que la page Parametres de l'admin les     */
/* liste et les modifie (PATCH /api/parametres/{cle}).          */
/* Sans effet si une cle existe deja.                           */
/*==============================================================*/

INSERT INTO parametre_systeme (cle, valeur, type_valeur, description) VALUES
    ('TOLERANCE_LOCATION_JOURS', '4', 'ENTIER', 'Nombre de jours de tolérance avant qu''un loyer en retard soit marqué EN_RETARD'),
    ('TOLERANCE_MANDAT_JOURS', '10', 'ENTIER', 'Nombre de jours de tolérance avant qu''un reversement bailleur soit considéré en retard'),
    ('MEDIAS_RETENTION_JOURS', '30', 'ENTIER', 'Délai avant purge définitive d''un média supprimé'),
    ('MEDIAS_MAX_FICHIERS_PAR_LOT', '10', 'ENTIER', 'Nombre maximum de fichiers par envoi groupé'),
    ('PENALITE_RETARD_MONTANT', '2000', 'DECIMAL', 'Montant fixe appliqué une seule fois par échéance de loyer passée en retard'),
    ('DELAI_EXPIRATION_RESERVATION_HEURES', '6', 'ENTIER', 'Délai après dateDebut au-delà duquel une réservation EN_ATTENTE non confirmée expire automatiquement')
ON CONFLICT (cle) DO NOTHING;
