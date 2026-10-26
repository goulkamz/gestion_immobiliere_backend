#!/bin/bash
# Restaure un fichier de sauvegarde dans une base TEMPORAIRE et compare avec la base en service :
# même nombre de tables et mêmes nombres de lignes sur les tables clés. La base temporaire est supprimée ensuite.
# Usage : verifier-restauration.sh /backups/postgres/gestion_immobiliere_AAAAMMJJ_HHMMSS.dump
set -uo pipefail

fichier="${1:?fichier de sauvegarde requis}"
test_db=restauration_test
export PGPASSWORD="${POSTGRES_PASSWORD}"
psql_admin() { psql -h postgres -U postgres -d postgres -v ON_ERROR_STOP=1 -tAq "$@"; }
psql_db()    { psql -h postgres -U postgres -d "$1" -v ON_ERROR_STOP=1 -tAq -c "$2"; }

psql_admin -c "DROP DATABASE IF EXISTS ${test_db}" -c "CREATE DATABASE ${test_db}" || exit 1
trap 'psql_admin -c "DROP DATABASE IF EXISTS ${test_db}" > /dev/null 2>&1' EXIT

if ! pg_restore -h postgres -U postgres -d "${test_db}" --no-owner --exit-on-error "${fichier}" > /dev/null 2>/tmp/restauration.err; then
  echo "ERREUR : la restauration de test a échoué"
  tail -5 /tmp/restauration.err
  exit 1
fi

ecart=0
requete_tables="SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'"
a=$(psql_db gestion_immobiliere "${requete_tables}")
b=$(psql_db "${test_db}" "${requete_tables}")
[ "$a" = "$b" ] || { echo "ERREUR : nombre de tables différent (${a} en service, ${b} restauré)"; ecart=1; }

for table in users maison contrat_mandat contra_location echeance_loyer paiement; do
  a=$(psql_db gestion_immobiliere "SELECT count(*) FROM ${table}")
  b=$(psql_db "${test_db}" "SELECT count(*) FROM ${table}")
  # Une écriture peut arriver entre la sauvegarde et la vérification : on tolère +/- 5 %
  if [ "$b" -gt "$a" ] || [ $((a - b)) -gt $((a / 20 + 1)) ]; then
    echo "ERREUR : ${table} : ${a} lignes en service, ${b} restaurées"
    ecart=1
  fi
done

if [ "$ecart" -eq 0 ]; then echo "Vérification de restauration : OK"; fi
exit "$ecart"
