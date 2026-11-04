# Mise en production : procédure complète

Ce document décrit, dans l'ordre, tout ce qu'il faut faire pour mettre l'application en ligne, la mettre à jour, la surveiller et la rétablir après un sinistre. Les détails des sauvegardes sont dans [sauvegardes.md](sauvegardes.md), ceux de la base dans [securite-base-de-donnees.md](securite-base-de-donnees.md).

## 1. Vue d'ensemble

| Service | Rôle | Exposition |
|---|---|---|
| `nginx` | Point d'entrée unique, HTTPS, cache des photos | Ports **80 et 443** publics |
| `frontend` | Site web (fichiers statiques) | Interne (aussi `127.0.0.1:5173`) |
| `backend` | API Spring Boot | Interne (aussi `127.0.0.1:8080`) |
| `postgres` | Base de données | `127.0.0.1:5432` seulement |
| `redis` | Sessions et caches | `127.0.0.1:6379` seulement |
| `minio` | Photos et documents PDF | `127.0.0.1:9000/9001` seulement |
| `certbot` | Renouvellement des certificats (profil `prod`) | Aucune |
| `sauvegarde` | Sauvegardes quotidiennes (profil `prod`) | Aucune |
| `redisinsight` | Outil d'inspection de Redis | **Ne pas démarrer en production** |

Deux domaines sont nécessaires : `WEB_DOMAIN` (le site) et `API_DOMAIN` (l'API pour le mobile). Sur `WEB_DOMAIN`, `/api/` va directement au backend : même origine pour le navigateur, cookies de session `Strict`.

## 2. Prérequis

### Serveur
- Linux avec **Docker Engine** et le plugin **Docker Compose**, `git`, `openssl` et `bash`.
- Pare-feu : n'ouvrir que **22** (SSH, idéalement limité à vos adresses), **80** et **443**. Les autres services sont déjà liés à `127.0.0.1` dans le `docker-compose.yml`, mais le pare-feu est une seconde barrière.
- Accès SSH par clé, connexion par mot de passe désactivée.

### Domaines
- Deux enregistrements DNS (type A, et AAAA si IPv6) : `WEB_DOMAIN` et `API_DOMAIN` vers l'adresse du serveur. Attendre la propagation avant l'étape 5 (`nslookup <domaine>`).

### Comptes externes
- **Un compte e-mail d'envoi** dédié à l'application (SMTP). Notez hôte, port (587), utilisateur, mot de passe.
- **Un stockage S3 hors du serveur** pour les sauvegardes (Backblaze B2, Wasabi, Scaleway, AWS S3...), avec une clé d'accès limitée à ce seul bucket.
- **Un service de supervision externe** (voir section 9).

## 3. Récupérer le code

Le build du frontend lit le dossier voisin `../frontend` : les deux dépôts doivent être côte à côte.

```
/opt/gestimmo/
├── gestionImmobiliere/   (backend, docker-compose.yml)
└── frontend/
```

```
mkdir -p /opt/gestimmo && cd /opt/gestimmo
git clone <url-du-depot-backend> gestionImmobiliere
git clone <url-du-depot-frontend> frontend
cd gestionImmobiliere
```

Toutes les commandes suivantes se lancent depuis `gestionImmobiliere/`.

## 4. Créer le fichier `.env`

1. `cp .env.prod.example .env`
2. Générer les secrets : `bash generer-secrets.sh` (rien n'est écrit sur le disque). Coller chaque ligne dans le `.env`.
3. Remplacer chaque `<...>` restant : domaines, e-mail, SMTP, bucket S3, identité de l'administrateur.
4. Restreindre les droits : `chmod 600 .env`.

### Contrôles obligatoires du `.env`

| Variable | Valeur attendue en production |
|---|---|
| `PROFILES_ACTIVE` | `prod` |
| `FLYWAY_LOCATIONS` | **Non définie** (sinon le seed de démonstration, qui vide les tables, serait exécuté) |
| `NOTIFICATIONS_EMAIL_REDIRECTION` | **Vide** (sinon tous les e-mails clients partent vers une seule adresse) |
| `LOGGING_LEVEL_ORG_HIBERNATE_TYPE_DESCRIPTOR_SQL_BASICBINDER` | **Non définie** (TRACE écrit les valeurs SQL dans les logs) |
| `JWT_COOKIE_SECURE` | `true` |
| `BASE_URL` | `https://<API_DOMAIN>` |
| `CORS_ALLOWED_ORIGINS` | `https://<WEB_DOMAIN>` seulement |
| `SECURITY_TRUSTED_PROXIES` | `172.28.0.10` (IP fixe de nginx dans `docker-compose.yml`) |
| `POSTGRES_PASSWORD` et `APP_DB_PASSWORD` | Deux valeurs **différentes**, générées |
| `MINIO_ROOT_USER` | Identifiant non standard (le script en génère un) |
| `BOOTSTRAP_ADMIN_PASSWORD` | 12 caractères minimum avec majuscule, minuscule, chiffre et symbole, sans espace |
| `TWOFA_ENCRYPTION_KEY` | Générée (ne jamais la perdre, voir ci-dessous) |

Vérification rapide qu'aucun `<` ne reste : `grep -n '<' .env` ne doit rien afficher (hors mot de passe contenant ce caractère).

### Garder une copie du `.env`

Les sauvegardes couvrent la base et les fichiers, **pas le `.env`**. Le conserver dans un gestionnaire de mots de passe ou un coffre chiffré, hors du serveur. Sa perte a des conséquences réelles :
- `TWOFA_ENCRYPTION_KEY` : les secrets de double authentification déjà enregistrés deviennent illisibles, les utilisateurs concernés ne peuvent plus se connecter avec leur code.
- `GI_BF_DOCUMENTS_SECRET` : la signature QR des documents déjà émis ne se vérifie plus.
- `JWT_SECRET_*`, `FINGER_SECRET_KEY` : tous les utilisateurs sont déconnectés (sans gravité, mais à savoir).
- `POSTGRES_PASSWORD`, `MINIO_ROOT_*`, `APP_DB_PASSWORD` : nécessaires pour reconnecter une base ou un stockage restaurés.

## 5. Premier démarrage

1. Vérifier que les DNS pointent bien sur le serveur et que les ports 80 et 443 sont ouverts.
2. Lancer le script de certificats (il génère un certificat provisoire, démarre nginx et ses dépendances, demande les vrais certificats Let's Encrypt pour les deux domaines, recharge nginx, puis démarre `certbot`) :
   ```
   ./nginx/init-letsencrypt.sh
   ```
3. Démarrer le reste, **en nommant les services** pour ne pas lancer `redisinsight` :
   ```
   docker compose --profile prod up -d --build postgres redis minio backend frontend nginx certbot sauvegarde
   ```
   Le premier build du backend et du frontend prend plusieurs minutes.
4. Au premier démarrage, Flyway crée le schéma (migrations V1 à V7, dont le rôle applicatif restreint `gestimmo_app`) et le backend crée le premier administrateur à partir des variables `BOOTSTRAP_ADMIN_*`.

### Vérifications immédiates

```
docker compose ps
docker logs gestion-immo-backend 2>&1 | grep -iE "Started|ERROR|Amorcage"
docker compose exec postgres psql -U postgres -d gestion_immobiliere -c "select version, description, success from flyway_schema_history order by installed_rank"
```

- Tous les services doivent être `Up` (les services avec contrôle d'état en `healthy`).
- Les 7 migrations doivent apparaître avec `success = t`.
- Le journal du backend doit montrer le démarrage de l'application, sans erreur.

## 6. Sécuriser le compte administrateur

1. Ouvrir `https://<WEB_DOMAIN>` et se connecter avec `BOOTSTRAP_ADMIN_EMAIL` et `BOOTSTRAP_ADMIN_PASSWORD`.
2. **Changer le mot de passe** depuis l'application, puis **activer la double authentification** sur ce compte.
3. **Retirer `BOOTSTRAP_ADMIN_PASSWORD` du `.env`** (laisser la ligne vide) et recréer le backend :
   ```
   docker compose up -d backend
   ```
   Le mot de passe de départ ne doit pas rester dans un fichier. Sans effet sur le compte déjà créé : l'amorçage ne fait rien si un administrateur actif existe.
4. Créer les autres comptes (agents) depuis l'application.

## 7. Première sauvegarde et exercice de restauration

La sauvegarde est lancée au démarrage seulement si `BACKUP_AU_DEMARRAGE=true`. Sinon, la faire à la main :

```
docker compose exec sauvegarde sauvegarde.sh
cat sauvegardes/dernier-statut.txt
docker logs gestion-immo-sauvegarde | grep ERREUR
```

Attendus : `dernier-statut.txt` commence par `OK`, aucune ligne `ERREUR`, et **aucun avertissement** sur la copie hors serveur. Contrôler ensuite dans le bucket S3 que les fichiers sont arrivés.

Faire alors **un exercice de restauration dans une base jetable** (commandes dans [sauvegardes.md](sauvegardes.md), section « S'entraîner sans risque »). Une sauvegarde jamais restaurée n'est pas une sauvegarde.

Si `BACKUP_AU_DEMARRAGE=true` a servi, le repasser à `false` ensuite.

## 8. Contrôles de recette avant ouverture au public

- [ ] `http://<WEB_DOMAIN>` redirige vers `https://` (code 301) : `curl -sI http://<WEB_DOMAIN>`
- [ ] Le certificat est valide pour les deux domaines (pas d'avertissement du navigateur).
- [ ] Depuis une autre machine, les ports 5432, 6379, 9000, 9001, 8080 et 5173 sont **fermés** : `nmap -Pn -p 22,80,443,5432,6379,9000,9001,8080,5173,5540 <ip-du-serveur>` ne doit montrer ouverts que 22, 80, 443.
- [ ] Connexion administrateur, avec code de double authentification.
- [ ] Création d'une annonce avec photo : l'image s'affiche sur le site public.
- [ ] Génération d'un document PDF, avec QR de vérification fonctionnel.
- [ ] Un e-mail de test arrive dans la boîte du **vrai destinataire** (inscription ou mot de passe oublié sur une adresse de test), pas dans une adresse redirigée.
- [ ] Aucune donnée de démonstration : la base ne contient que le compte administrateur et les paramètres par défaut.
- [ ] Les notifications planifiées partent à l'heure voulue (réglages `NOTIFICATIONS_CRON_AGENTS` et `NOTIFICATIONS_CRON_CLIENTS`, 07:00 et 07:15 par défaut).

## 9. Supervision

La surveillance interne (alerte aux administrateurs quand une sauvegarde échoue) est décrite dans [sauvegardes.md](sauvegardes.md). Elle ne peut pas signaler une panne du serveur entier : il faut une supervision **externe**.

### Surveillance du site (à configurer)
Créer chez un service gratuit (UptimeRobot, Better Stack) deux vérifications HTTPS toutes les 5 minutes, avec alerte par e-mail et par SMS ou notification mobile :
- `https://<WEB_DOMAIN>/` : le site répond.
- `https://<API_DOMAIN>/api/stats/public` : le backend et la base répondent. **Choisir la méthode GET dans l'outil** : en HEAD, cette adresse renvoie 401 (vérifié le 04/11/2026, la règle de sécurité ne couvre que GET) et la supervision signalerait une panne à tort. À tester d'abord : `curl -s -o /dev/null -w '%{http_code}' https://<API_DOMAIN>/api/stats/public` doit afficher 200. Autre choix possible en HEAD : `/api/public/annonces`, qui répond 200.

Le projet n'a pas de point de santé dédié (Spring Actuator absent). Si un jour on veut vérifier précisément la base, Redis et MinIO, il faudra l'ajouter.

### Alerte sur absence de sauvegarde (reste à faire)
Un signal de vie Healthchecks.io envoyé à la fin de chaque sauvegarde réussie permettrait d'être prévenu si le serveur est mort **ou** si la sauvegarde ne tourne plus. Il n'est pas encore implémenté : prévoir une variable `BACKUP_PING_URL` appelée par `sauvegarde/sauvegarde.sh`.

### Contrôles à la main
- Chaque semaine : `cat sauvegardes/dernier-statut.txt`, et la présence des fichiers récents dans le bucket S3.
- Espace disque : `df -h` et `docker system df`. Les dumps, les photos MinIO et les images Docker grossissent.
- Chaque trimestre : exercice de restauration.

## 10. Mettre à jour l'application

1. Faire une sauvegarde juste avant : `docker compose exec sauvegarde sauvegarde.sh`, puis vérifier `OK` dans `sauvegardes/dernier-statut.txt`.
2. Récupérer le code :
   ```
   git pull
   git -C ../frontend pull
   ```
3. Reconstruire et relancer les services modifiés :
   ```
   docker compose --profile prod build backend frontend
   docker compose --profile prod up -d backend frontend
   ```
   Un changement du backend nécessite toujours ce rebuild : redémarrer sans reconstruire garde l'ancienne version.
4. Les nouvelles migrations Flyway s'appliquent seules au démarrage du backend. Contrôler :
   ```
   docker logs gestion-immo-backend 2>&1 | grep -iE "Started|ERROR|flyway"
   ```
5. Parcourir rapidement l'application (connexion, une liste, un document).

### Si la mise à jour échoue
Flyway ne sait pas annuler une migration. Pour revenir en arrière :
1. `docker compose stop backend`
2. Revenir au code précédent (`git checkout <commit-précédent>`) puis rebuild du backend.
3. Si une migration avait déjà modifié la base, restaurer le dump pris à l'étape 1 (section 11), puis `docker compose start backend`.

## 11. Après un sinistre

### Panne simple (un service planté)
`docker compose ps`, puis `docker compose logs --tail 100 <service>` et `docker compose up -d <service>`. Tous les services ont `restart: unless-stopped` : ils repartent seuls au redémarrage du serveur, à condition que Docker démarre au boot (`systemctl enable docker`).

### Serveur perdu
1. Préparer un nouveau serveur (section 2) et faire pointer les DNS dessus.
2. Cloner les deux dépôts (section 3).
3. Reposer le **`.env` sauvegardé** (section 4). Sans lui, les secrets 2FA et la signature des documents sont perdus.
4. Récupérer les sauvegardes depuis le bucket S3 dans `sauvegardes/` (mêmes sous-dossiers `postgres/` et `minio/`).
5. Obtenir les certificats : `./nginx/init-letsencrypt.sh`.
6. Démarrer d'abord la base et le stockage : `docker compose --profile prod up -d --build postgres minio sauvegarde`.
7. Restaurer (procédure exacte dans [sauvegardes.md](sauvegardes.md), section « Restauration après un sinistre ») :
   ```
   docker compose exec -e CONFIRMER=oui sauvegarde restaurer.sh base dernier
   docker compose exec sauvegarde restaurer.sh fichiers
   ```
8. Démarrer le reste : `docker compose --profile prod up -d --build redis backend frontend nginx certbot`.
9. Refaire les contrôles de la section 8. Les utilisateurs devront se reconnecter (les sessions Redis ne sont pas sauvegardées).

Perte maximale de données : jusqu'à 24 h (sauvegarde quotidienne).

## 12. Entretien courant

| Tâche | Fréquence |
|---|---|
| Vérifier `dernier-statut.txt` et le bucket S3 | Chaque semaine |
| Mises à jour de sécurité du système (`apt upgrade`) puis redémarrage si nécessaire | Chaque mois |
| Mise à jour des images Docker (`docker compose pull`, puis `up -d`) pour postgres, redis, minio, nginx | Chaque trimestre, après une sauvegarde |
| Exercice de restauration | Chaque trimestre |
| Contrôle du renouvellement des certificats (`docker logs gestion-immo-certbot`) | Chaque trimestre |
| Revue des comptes administrateurs et agents | Chaque trimestre |
| Nettoyage des images inutilisées : `docker image prune` | Au besoin |

## 13. Aide-mémoire

```
docker compose ps                                  état des services
docker compose logs -f --tail 100 backend          suivre le journal du backend
docker compose restart backend                     redémarrer sans reconstruire
docker compose --profile prod up -d --build backend   reconstruire et relancer
docker compose exec sauvegarde sauvegarde.sh       sauvegarde immédiate
cat sauvegardes/dernier-statut.txt                 état de la dernière sauvegarde
docker compose exec postgres psql -U postgres -d gestion_immobiliere   console SQL
```

## 14. Ce qui reste à faire avant la mise en ligne

- [ ] Choisir et configurer le stockage S3 hors serveur.
- [ ] Choisir le service de supervision externe et créer les deux vérifications.
- [ ] Ajouter le signal de vie `BACKUP_PING_URL` (section 9).
- [ ] Remplacer les photos et images de démonstration par les vraies (dossier `images-a-fournir`).
- [ ] Faire relire cette procédure en la déroulant une fois sur un serveur de test.

## 15. Préproduction (recette externe et scans de sécurité)

La préproduction est un second environnement, identique à la production dans sa construction, réservé à une équipe de test externe et aux scans de sécurité. Elle se monte avec les sections 2 à 8, avec les écarts ci-dessous. **Ne jamais y mettre de vraies données de clients.**

### Serveur et domaines
- Un serveur **distinct** de la production (VPS payant, 4 Go de RAM minimum : le build du backend et du frontend, Postgres, MinIO et nginx tournent ensemble).
- Deux domaines **propres à la préproduction** (par exemple `recette.<domaine>` et `api-recette.<domaine>`), jamais ceux de la production. Les valeurs `WEB_DOMAIN`, `API_DOMAIN`, `BASE_URL` et `CORS_ALLOWED_ORIGINS` du `.env` pointent sur eux.
- Tous les secrets du `.env` sont **générés à nouveau** (`generer-secrets.sh`) : aucune valeur partagée avec la production.

### Écarts du `.env` par rapport à la production

| Variable | Valeur en préproduction | Raison |
|---|---|---|
| `NOTIFICATIONS_EMAIL_REDIRECTION` | **Renseignée** (une boîte que vous contrôlez) | Les testeurs créent des comptes et déclenchent des e-mails : aucun message ne doit partir vers de vraies adresses. C'est l'inverse de la production |
| `FLYWAY_LOCATIONS` | `classpath:db/migration,classpath:db/seed` si des données de démonstration sont voulues, sinon non définie | Voir « Données de test » |
| `JWT_COOKIE_SECURE` | `true` | HTTPS reste obligatoire, comme en production |
| `PROFILES_ACTIVE` | `prod` | On teste la configuration réelle, pas celle du développement |

### Données de test
- Le seed `R__seed_dev.sql` **vide les tables (TRUNCATE)** à chaque fois que son contenu change : une mise à jour qui le modifie effacerait tout ce que les testeurs ont saisi. Avant chaque mise à jour, vérifier si ce fichier a changé (`git diff <ancienne-version> -- src/main/resources/db/seed`) et prévenir les testeurs si c'est le cas.
- Les comptes du seed utilisent le mot de passe `Password123!`, connu de tous les dépôts. **Ne pas le laisser tel quel** sur un serveur joignable depuis Internet : remplacer les mots de passe de chaque compte remis aux testeurs (depuis l'application, ou en leur faisant utiliser « mot de passe oublié »), et supprimer les comptes inutiles.
- Donner à chaque testeur un compte nominatif par rôle utile (client, bailleur, agent, administrateur), avec un mot de passe distinct.
- Les testeurs ne doivent pas avoir d'accès SSH ni Docker. Ils passent par le site.

### Restreindre l'accès
La préproduction ne doit pas être indexée ni ouverte à tout Internet. Au choix, devant `nginx` :
- **Restriction par adresse IP** (à préférer si les testeurs ont des adresses fixes) : directives `allow <ip>;` puis `deny all;` dans les blocs `server` de `nginx/templates`.
- **Authentification HTTP** : `auth_basic` avec un fichier `htpasswd`, un identifiant par testeur. Attention : elle s'ajoute à la connexion de l'application, et l'application mobile ou un scanner doivent aussi pouvoir la passer.
- Dans tous les cas, ajouter l'en-tête `X-Robots-Tag: noindex, nofollow` pour que les moteurs de recherche ignorent le site.

Les adresses des machines de scan de sécurité doivent être autorisées explicitement. La limite de connexion de l'application (blocage après échecs répétés) restera active pendant les scans : prévoir avec l'équipe quels comptes servent aux tests d'authentification, ou la desserrer temporairement pour eux seulement.

### Avant d'ouvrir aux testeurs
- [ ] Les contrôles de la section 8 sont passés, à l'exception de ceux sur les données de démonstration et sur l'e-mail réel (remplacés par : un e-mail de test arrive bien dans la boîte de redirection).
- [ ] Aucun mot de passe `Password123!` ne reste sur un compte accessible.
- [ ] L'accès est restreint (IP ou authentification HTTP) et le noindex est en place.
- [ ] Une sauvegarde est configurée, ou la perte des données de test est acceptée par tous.
- [ ] Un document partagé avec les testeurs précise : périmètre testé, comptes fournis, adresse de signalement des anomalies, et interdiction d'y saisir des données réelles.
