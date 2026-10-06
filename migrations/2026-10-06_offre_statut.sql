-- ==============================================================
-- Migration : offre.statut ACTIVE / EXPIREE / SUSPENDUE -> EN_ATTENTE / TRAITEE / REFUSEE
-- StatutOffre est devenu un statut de traitement (voir StatutOffre.java). Sans cette migration,
-- le backend ne peut plus lire les anciennes lignes (valeur d'enum inconnue).
-- Conversion : toutes les anciennes valeurs -> EN_ATTENTE (aucune offre n'avait ete "traitee" au
-- sens metier ; l'agent requalifie ensuite en TRAITEE / REFUSEE via PATCH /api/offres/{id}/statut).
-- Idempotent : sans effet une fois les lignes converties.
--
-- Usage :
--   docker compose exec -T postgres psql -U postgres -d gestion_immobiliere < migrations/2026-10-06_offre_statut.sql
-- ==============================================================
BEGIN;

UPDATE offre SET statut = 'EN_ATTENTE'
WHERE statut IS NULL OR statut IN ('ACTIVE', 'EXPIREE', 'SUSPENDUE');

COMMIT;

-- Verification :
--   SELECT statut, count(*) FROM offre GROUP BY statut;
