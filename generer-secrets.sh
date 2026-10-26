#!/usr/bin/env bash
# Affiche des secrets aleatoires a copier dans le .env de production.
# Rien n'est ecrit sur le disque : a coller directement dans le .env du serveur.
set -euo pipefail

hex() { openssl rand -hex "$1"; }
b64() { openssl rand -base64 "$1" | tr -d '\n'; }

echo "JWT_SECRET_ACCESS=$(b64 64)"
echo "JWT_SECRET_REFRESH=$(b64 64)"
echo "FINGER_SECRET_KEY=$(b64 32)"
echo "GI_BF_DOCUMENTS_SECRET=$(b64 32)"
echo "POSTGRES_PASSWORD=$(hex 24)"
echo "REDIS_PASSWORD=$(hex 24)"
echo "MINIO_ROOT_USER=minio-$(hex 4)"
echo "MINIO_ROOT_PASSWORD=$(hex 24)"
