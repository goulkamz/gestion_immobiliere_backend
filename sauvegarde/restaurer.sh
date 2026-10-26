#!/bin/bash
# Restauration en cas de sinistre. ARRÊTER LE BACKEND AVANT (docker compose stop backend).
#   restaurer.sh base <fichier.dump|dernier>   remplace TOUTE la base en service par la sauvegarde
#   restaurer.sh fichiers                      recopie les fichiers MinIO depuis la sauvegarde
# La restauration de la base exige CONFIRMER=oui dans l'environnement (opération destructive).
# BASE_CIBLE (défaut gestion_immobiliere) permet d'essayer la restauration sur une base jetable.
set -uo pipefail

DEST=/backups
CIBLE_DB="${BASE_CIBLE:-gestion_immobiliere}"
export PGPASSWORD="${POSTGRES_PASSWORD}"

case "${1:-}" in
  base)
    cible="${2:-dernier}"
    if [ "${cible}" = "dernier" ]; then
      cible=$(ls -1t "${DEST}"/postgres/gestion_immobiliere_*.dump 2>/dev/null | head -1)
    fi
    [ -f "${cible}" ] || { echo "Fichier de sauvegarde introuvable : ${cible}"; exit 1; }
    [ "${CONFIRMER:-}" = "oui" ] || { echo "Opération destructive. Relancer avec CONFIRMER=oui pour remplacer la base par : ${cible}"; exit 1; }
    if [ -f "${cible}.sha256" ]; then (cd "$(dirname "${cible}")" && sha256sum -c "$(basename "${cible}").sha256") || exit 1; fi
    echo "Restauration de la base ${CIBLE_DB} depuis ${cible}..."
    psql -h postgres -U postgres -d postgres -v ON_ERROR_STOP=1 -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '${CIBLE_DB}' AND pid <> pg_backend_pid()" > /dev/null
    psql -h postgres -U postgres -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS ${CIBLE_DB}" -c "CREATE DATABASE ${CIBLE_DB}"
    pg_restore -h postgres -U postgres -d "${CIBLE_DB}" --no-owner --exit-on-error "${cible}" && echo "Base restaurée. Redémarrer le backend : docker compose start backend"
    ;;
  fichiers)
    mc alias set local http://minio:9000 "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}" > /dev/null || exit 1
    mc mirror --overwrite "${DEST}/minio" local && echo "Fichiers MinIO restaurés."
    ;;
  *)
    echo "Usage : restaurer.sh base <fichier|dernier>  |  restaurer.sh fichiers"
    exit 1
    ;;
esac
