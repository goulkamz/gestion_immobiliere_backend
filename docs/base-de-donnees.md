# Base de données : migrations Flyway

La structure de la base est gérée par **Flyway**. Il s'exécute au démarrage du backend, avant la validation Hibernate (`ddl-auto=validate`), et note ce qu'il a appliqué dans la table `flyway_schema_history`.

## Où sont les fichiers

| Fichier | Rôle |
|---|---|
| `src/main/resources/db/migration/V1__schema_initial.sql` | Schéma complet (tables, séquences, contraintes, index) |
| `src/main/resources/db/migration/V2__triggers_cascade_soft_delete.sql` | Triggers de suppression/restauration en cascade |
| `src/main/resources/db/migration/V3__parametres_systeme_defaut.sql` | Les 6 paramètres système par défaut (données de référence, aussi en production) |
| `src/main/resources/db/seed/R__seed_dev.sql` | Données de démonstration (comptes `admin@`, `agent@`, `bailleur@gestimmo.test`, mot de passe `Password123!`) |

## Ajouter une évolution du schéma

1. Créer un **nouveau** fichier `src/main/resources/db/migration/V4__description_courte.sql` (V3 existe déjà ; ensuite `V5__`, `V6__`... : toujours le numéro suivant, deux underscores).
2. Y mettre seulement la modification (`ALTER TABLE ...`, `CREATE TABLE ...`), si possible rejouable sans danger.
3. Redémarrer le backend (`docker compose build backend && docker compose up -d backend`) : Flyway l'applique, puis Hibernate vérifie que les entités correspondent.

**Ne jamais modifier une migration déjà appliquée** (V1, V2, V3...) : Flyway compare une empreinte (checksum) et refuse de démarrer si un fichier a changé. Une erreur se corrige par une nouvelle migration.

## Données de démonstration (seed)

- Le seed est une migration **répétable** (`R__`) : elle passe après toutes les migrations versionnées, s'exécute une fois sur une base neuve, et ne se rejoue que si le fichier est modifié (elle vide alors les tables avant de réinsérer).
- Il n'est chargé que par les profils `dev` (`application-dev.properties`) et `test` (`application-test.properties`), via `spring.flyway.locations=classpath:db/migration,classpath:db/seed`.
- **En production**, ne pas utiliser le profil `dev` (`PROFILES_ACTIVE=prod`) : la configuration par défaut ne charge que `db/migration`, donc aucun compte de test n'est créé.

## Repartir d'une base vide (dev)

```
docker compose down
docker volume rm gestionimmobiliere_postgres-data
docker compose up -d
```

Flyway recrée tout au démarrage du backend.

## Base existante créée avant Flyway

Non prévu : le projet est reparti d'une base neuve (octobre 2026). Une base de production existante aurait besoin d'une « baseline » (`spring.flyway.baseline-on-migrate=true` avec `baseline-version`), à décider à ce moment-là.

## Compte de connexion

Flyway utilise aujourd'hui le même compte que l'application (`postgres`). Pour un rôle applicatif restreint, donner à Flyway son propre compte avec `spring.flyway.user` et `spring.flyway.password`.
