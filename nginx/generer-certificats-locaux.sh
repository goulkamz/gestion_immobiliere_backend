#!/usr/bin/env bash
# Certificats auto-signes pour tester HTTPS en local (le navigateur affichera un
# avertissement, a accepter une fois). Ranges au meme endroit que ceux de
# Let's Encrypt, pour que nginx utilise la meme configuration en local et en prod.
# Usage : ./nginx/generer-certificats-locaux.sh   (depuis la racine du projet)
set -euo pipefail
export MSYS_NO_PATHCONV=1   # Git Bash : ne pas convertir "/CN=..." en chemin Windows

cd "$(dirname "$0")/.."
# Lecture ciblee : .env contient des secrets avec $ ( ) que "source" interpreterait
lire_env() { [ -f .env ] && grep -E "^$1=" .env | tail -1 | cut -d= -f2- | tr -d $'\r' || true; }
API_DOMAIN="$(lire_env API_DOMAIN)"; API_DOMAIN="${API_DOMAIN:-api.gestimmo.localhost}"
MEDIAS_DOMAIN="$(lire_env MEDIAS_DOMAIN)"; MEDIAS_DOMAIN="${MEDIAS_DOMAIN:-medias.gestimmo.localhost}"

for domaine in "$API_DOMAIN" "$MEDIAS_DOMAIN"; do
    dossier="nginx/letsencrypt/live/$domaine"
    mkdir -p "$dossier"
    openssl req -x509 -nodes -newkey rsa:2048 -days 825 \
        -keyout "$dossier/privkey.pem" -out "$dossier/fullchain.pem" \
        -subj "/CN=$domaine" -addext "subjectAltName=DNS:$domaine" 2>/dev/null
    echo "Certificat auto-signe cree : $dossier"
done
