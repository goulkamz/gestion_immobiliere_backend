#!/bin/bash
# Sauvegarde complète : base PostgreSQL (pg_dump), fichiers MinIO (copie miroir), vérification par restauration
# dans une base temporaire, rotation, puis copie hors du serveur si BACKUP_S3_ENDPOINT est renseigné.
# Code de sortie 0 = tout est bon. Le résultat est écrit dans /backups/dernier-statut.txt.
set -uo pipefail

DEST=/backups
JOURS="${BACKUP_RETENTION_DAYS:-14}"
horodatage=$(date +%Y%m%d_%H%M%S)
fichier="${DEST}/postgres/gestion_immobiliere_${horodatage}.dump"
statut=OK

mkdir -p "${DEST}/postgres" "${DEST}/minio"
export PGPASSWORD="${POSTGRES_PASSWORD}"

echo "[$(date -Is)] Sauvegarde de la base..."
if pg_dump -h postgres -U postgres -d gestion_immobiliere -Fc -f "${fichier}.partiel"; then
  mv "${fichier}.partiel" "${fichier}"
  (cd "${DEST}/postgres" && sha256sum "$(basename "${fichier}")" > "$(basename "${fichier}").sha256")
  if ! pg_restore --list "${fichier}" > /dev/null; then
    echo "ERREUR : le fichier de sauvegarde est illisible"
    statut=ECHEC
  fi
else
  echo "ERREUR : pg_dump a échoué"
  rm -f "${fichier}.partiel"
  statut=ECHEC
fi

echo "[$(date -Is)] Copie des fichiers MinIO..."
if mc alias set local http://minio:9000 "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}" > /dev/null; then
  # Sans --remove : un fichier supprimé par erreur dans MinIO reste dans la copie
  mc mirror --overwrite --quiet local "${DEST}/minio" || { echo "ERREUR : copie MinIO"; statut=ECHEC; }
else
  echo "ERREUR : connexion à MinIO impossible"
  statut=ECHEC
fi

if [ -f "${fichier}" ]; then
  echo "[$(date -Is)] Vérification par restauration..."
  /usr/local/bin/verifier-restauration.sh "${fichier}" || statut=ECHEC
fi

# Rotation locale : on ne garde que les JOURS derniers jours de sauvegardes de la base
find "${DEST}/postgres" -name 'gestion_immobiliere_*' -mtime "+${JOURS}" -delete

# Copie hors du serveur (stockage compatible S3 : Backblaze B2, Wasabi, Scaleway, AWS, etc.)
if [ -n "${BACKUP_S3_ENDPOINT:-}" ]; then
  echo "[$(date -Is)] Copie hors serveur vers ${BACKUP_S3_ENDPOINT}..."
  if mc alias set distant "${BACKUP_S3_ENDPOINT}" "${BACKUP_S3_ACCESS_KEY}" "${BACKUP_S3_SECRET_KEY}" > /dev/null \
     && mc mirror --overwrite --quiet "${DEST}" "distant/${BACKUP_S3_BUCKET}"; then
    mc rm --recursive --force --older-than "${JOURS}d" "distant/${BACKUP_S3_BUCKET}/postgres" > /dev/null 2>&1 || true
  else
    echo "ERREUR : copie hors serveur"
    statut=ECHEC
  fi
else
  echo "AVERTISSEMENT : BACKUP_S3_ENDPOINT non défini, les sauvegardes restent sur ce serveur (pas de protection contre une panne de la machine)."
fi

echo "${statut} $(date -Is) ${fichier}" > "${DEST}/dernier-statut.txt"
echo "[$(date -Is)] Sauvegarde terminée : ${statut}"
[ "${statut}" = "OK" ]
