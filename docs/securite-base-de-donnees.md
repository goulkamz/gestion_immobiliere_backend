# Sécurité de la base de données

## Deux comptes, deux rôles

| Compte | Utilisé par | Droits |
|---|---|---|
| `postgres` | Flyway (migrations), service de sauvegarde, administration | Propriétaire, tous les droits |
| `gestimmo_app` | L'application (Spring Boot) | Lecture et écriture des données **uniquement** |

`gestimmo_app` est créé par la migration `V4__role_applicatif_restreint.sql`. Il ne peut pas créer ni supprimer de table, pas créer de compte, pas faire de `TRUNCATE`, et ne voit pas l'historique des migrations. Si l'application est compromise, l'attaquant ne peut ni détruire le schéma ni administrer le serveur de base.

Le mot de passe vient de `APP_DB_PASSWORD` (`.env`), passé à Flyway par le placeholder `apppassword` : il n'est écrit dans aucun fichier du dépôt. Changer le mot de passe plus tard : `ALTER ROLE gestimmo_app PASSWORD '...'` puis mettre à jour `APP_DB_PASSWORD` et redémarrer le backend.

Les tables créées par les migrations suivantes sont accessibles d'office au rôle applicatif (droits par défaut), à condition que les migrations s'exécutent avec le même compte propriétaire.

## Preuves financières immuables

La migration `V5__immutabilite_preuves_financieres.sql` pose un trigger sur `paiement`, `paiement_echeance`, `paiement_location_bien_service`, `remboursement` et `journal_operation`. Sur ces tables, quel que soit le compte :
- **aucune ligne ne peut être supprimée** (ni physiquement, ni par suppression logique) ;
- **aucune ligne ne peut être modifiée**, sauf la date `updated_at`.

Une erreur se corrige par une nouvelle ligne (remboursement, paiement inverse), jamais en réécrivant l'historique. Les triggers de cascade existants excluent déjà ces tables.

Maintenance exceptionnelle (à faire par un administrateur de la base, avec une trace écrite) :

```sql
BEGIN;
SET LOCAL gestimmo.maintenance = 'on';
-- correction ciblée
COMMIT;
```

L'application ne positionne jamais ce réglage. Le `TRUNCATE` (jeu de données de développement) ne déclenche pas ces triggers et n'est pas accordé au rôle applicatif.

## Contrainte sur les échéances

`chk_echeance_paye_montant` : une échéance ne peut pas passer à `PAYE` si le montant payé est inférieur au montant dû plus la pénalité.

## Tests

`SecuriteBaseDonneesIntegrationTest` vérifie le rôle restreint et l'immutabilité. Toute la suite d'intégration tourne avec `gestimmo_app` : un droit manquant se verrait immédiatement.
