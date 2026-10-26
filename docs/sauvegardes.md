# Sauvegardes et restauration

Ce qui doit survivre à une panne : la **base PostgreSQL** (contrats, loyers, paiements, utilisateurs) et les **fichiers MinIO** (photos, documents PDF). Flyway protège le schéma, pas les données : sans sauvegarde, une base reconstruite est vide.

## Ce que fait le service `sauvegarde`

Chaque jour à `BACKUP_HEURE` (02:00, fuseau `BACKUP_TZ`) :

1. **Base** : `pg_dump` au format compressé dans `sauvegardes/postgres/gestion_immobiliere_AAAAMMJJ_HHMMSS.dump`, avec son empreinte `.sha256`.
2. **Fichiers** : copie miroir de MinIO dans `sauvegardes/minio/`. Un fichier supprimé par erreur dans MinIO reste dans la copie.
3. **Vérification** : le dump est restauré dans une base temporaire (`restauration_test`), puis le nombre de tables et de lignes des tables clés est comparé à la base en service. La base temporaire est supprimée.
4. **Rotation** : les dumps de plus de `BACKUP_RETENTION_DAYS` jours (14 par défaut) sont supprimés.
5. **Copie hors serveur** (si `BACKUP_S3_ENDPOINT` est défini) : tout le dossier est copié vers un stockage compatible S3.
6. **Résultat** : `sauvegardes/dernier-statut.txt` contient `OK` ou `ECHEC`, la date et le fichier produit.

Le dossier `sauvegardes/` est ignoré par git.

## Mise en route (production)

1. Renseigner les variables `BACKUP_*` dans le `.env` (modèle : `.env.prod.example`).
2. Démarrer : `docker compose --profile prod up -d sauvegarde`
3. Premier essai immédiat : `docker compose exec sauvegarde sauvegarde.sh`
4. Vérifier : `cat sauvegardes/dernier-statut.txt` doit afficher `OK`, et `docker logs gestion-immo-sauvegarde` ne doit contenir aucune ligne `ERREUR`.

### La copie hors serveur est indispensable

Une sauvegarde qui reste sur la machine ne protège pas d'une panne de cette machine (disque, incendie, piratage). Choisir un stockage distant compatible S3 (Backblaze B2, Wasabi, Scaleway, AWS S3...) et renseigner `BACKUP_S3_ENDPOINT`, `BACKUP_S3_BUCKET`, `BACKUP_S3_ACCESS_KEY` et `BACKUP_S3_SECRET_KEY`. Créer pour cela une clé d'accès limitée à ce seul bucket. Sans ces variables, le service écrit un avertissement à chaque passage.

## Restauration après un sinistre

À faire sur le serveur, dans le dossier du `docker-compose.yml`.

1. Arrêter le backend : `docker compose stop backend`
2. S'il faut récupérer les sauvegardes depuis le stockage distant, les copier dans `sauvegardes/` (même arborescence : `postgres/`, `minio/`).
3. Restaurer la base (opération destructive, elle remplace toute la base) :
   `docker compose exec -e CONFIRMER=oui sauvegarde restaurer.sh base dernier`
   Pour un fichier précis : `... restaurer.sh base /backups/postgres/gestion_immobiliere_20261026_105147.dump`
4. Restaurer les fichiers si nécessaire : `docker compose exec sauvegarde restaurer.sh fichiers`
5. Redémarrer : `docker compose start backend`

Sans `CONFIRMER=oui`, le script refuse de s'exécuter. L'empreinte `.sha256` est contrôlée avant la restauration.

### S'entraîner sans risque

`BASE_CIBLE` permet de restaurer dans une base jetable, sans toucher à la base en service :

```
docker compose exec -e CONFIRMER=oui -e BASE_CIBLE=essai_sinistre sauvegarde restaurer.sh base dernier
docker compose exec postgres psql -U postgres -d essai_sinistre -c "select count(*) from users"
docker compose exec postgres psql -U postgres -d postgres -c "DROP DATABASE essai_sinistre"
```

Faire cet essai après toute modification du service, et au moins une fois par trimestre.

## Test réalisé (26/10/2026)

Sauvegarde complète : base (122 Ko), fichiers MinIO (28,8 Mo), vérification par restauration OK. Restauration du dernier dump dans une base jetable : 19 utilisateurs, 50 maisons, 2 mandats et 6 migrations Flyway retrouvés, identiques à la base en service.

## Limites connues

- Les sauvegardes sont quotidiennes : en cas de sinistre, jusqu'à 24 h de données peuvent être perdues. Pour réduire cette fenêtre, augmenter la fréquence (adapter `planificateur.sh`) ou activer l'archivage continu de PostgreSQL (WAL).
- Les sessions Redis et les caches ne sont pas sauvegardés : ils se reconstruisent (les utilisateurs devront se reconnecter).
- Aucune alerte n'est envoyée en cas d'échec : surveiller `dernier-statut.txt` (par exemple par une supervision externe qui vérifie que la date est récente et que la ligne commence par `OK`).
