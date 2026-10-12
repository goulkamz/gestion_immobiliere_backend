-- ==============================================================
-- Migration : contrat_mandat.mode_facturation texte libre -> enum ModeFacturation
-- Valeurs : MENSUEL / ANNUEL (GESTION), COMMISSION (LOCATION et VENTE).
-- Sans cette migration, le backend ne peut plus lire les anciennes lignes (valeur d'enum inconnue).
-- Conversion :
--   GESTION  : texte contenant "annu" -> ANNUEL, sinon (y compris vide) -> MENSUEL
--   LOCATION / VENTE : toujours COMMISSION
-- Idempotent : sans effet une fois les lignes converties.
--
-- Usage :
--   docker compose exec -T postgres psql -U postgres -d gestion_immobiliere < migrations/2026-10-12_mandat_mode_facturation.sql
-- ==============================================================
BEGIN;

UPDATE contrat_mandat SET mode_facturation = 'ANNUEL'
WHERE type_mandat = 'GESTION' AND mode_facturation ILIKE '%annu%' AND mode_facturation <> 'ANNUEL';

UPDATE contrat_mandat SET mode_facturation = 'MENSUEL'
WHERE type_mandat = 'GESTION' AND (mode_facturation IS NULL OR mode_facturation NOT IN ('MENSUEL', 'ANNUEL'));

UPDATE contrat_mandat SET mode_facturation = 'COMMISSION'
WHERE type_mandat IN ('LOCATION', 'VENTE') AND (mode_facturation IS NULL OR mode_facturation <> 'COMMISSION');

COMMIT;

-- Verification :
--   SELECT type_mandat, mode_facturation, count(*) FROM contrat_mandat GROUP BY 1, 2;
