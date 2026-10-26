#!/bin/bash
# Lance sauvegarde.sh chaque jour à BACKUP_HEURE (fuseau TZ). Un échec n'arrête pas le planificateur.
set -u

HEURE="${BACKUP_HEURE:-02:00}"
echo "Planificateur de sauvegarde : chaque jour à ${HEURE} (fuseau ${TZ:-UTC}), conservation ${BACKUP_RETENTION_DAYS:-14} jours."

if [ "${BACKUP_AU_DEMARRAGE:-false}" = "true" ]; then
  /usr/local/bin/sauvegarde.sh || echo "ECHEC de la sauvegarde de démarrage"
fi

while true; do
  maintenant=$(date +%s)
  cible=$(date -d "today ${HEURE}" +%s)
  if [ "$cible" -le "$maintenant" ]; then cible=$(date -d "tomorrow ${HEURE}" +%s); fi
  attente=$((cible - maintenant))
  echo "Prochaine sauvegarde dans ${attente} s"
  sleep "$attente"
  /usr/local/bin/sauvegarde.sh || echo "ECHEC de la sauvegarde, voir /backups/dernier-statut.txt"
done
