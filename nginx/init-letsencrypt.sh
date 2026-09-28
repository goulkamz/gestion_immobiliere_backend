#!/usr/bin/env bash
# Production : remplace les certificats auto-signes par de vrais certificats
# Let's Encrypt. A lancer UNE fois sur le serveur, apres avoir :
#   1. fait pointer le DNS de API_DOMAIN vers le serveur ;
#   2. renseigne API_DOMAIN et LETSENCRYPT_EMAIL dans .env ;
#   3. ouvert les ports 80 et 443.
# Le renouvellement est ensuite automatique (service certbot, profil "prod").
# Usage : ./nginx/init-letsencrypt.sh   (depuis la racine du projet)
set -euo pipefail

cd "$(dirname "$0")/.."
# Lecture ciblee : .env contient des secrets avec $ ( ) que "source" interpreterait
lire_env() { grep -E "^$1=" .env | tail -1 | cut -d= -f2- | tr -d $'\r' || true; }
API_DOMAIN="$(lire_env API_DOMAIN)"
LETSENCRYPT_EMAIL="$(lire_env LETSENCRYPT_EMAIL)"
: "${API_DOMAIN:?API_DOMAIN manquant dans .env}"
: "${LETSENCRYPT_EMAIL:?LETSENCRYPT_EMAIL manquant dans .env}"

# nginx a besoin d'un certificat (meme provisoire) pour demarrer et servir le challenge
./nginx/generer-certificats-locaux.sh
docker compose up -d nginx

for domaine in "$API_DOMAIN"; do
    # Sinon certbot cree "<domaine>-0001" a cote du certificat provisoire
    rm -rf "nginx/letsencrypt/live/$domaine" "nginx/letsencrypt/archive/$domaine" \
           "nginx/letsencrypt/renewal/$domaine.conf"
    docker compose run --rm --entrypoint certbot certbot certonly \
        --webroot -w /var/www/acme -d "$domaine" \
        --email "$LETSENCRYPT_EMAIL" --agree-tos --no-eff-email --non-interactive
done

docker compose exec nginx nginx -s reload
docker compose --profile prod up -d certbot
echo "Certificats Let's Encrypt installes ; renouvellement automatique actif."
