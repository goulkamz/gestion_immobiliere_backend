-- ==============================================================
-- Migration : mode de facturation ANNUEL renommé CUMUL (mandats GESTION)
-- Le mode CUMUL permet de verser au bailleur plusieurs mois d'un coup
-- (annuel, trimestriel, semestriel : l'agent choisit jusqu'à quel mois).
-- Idempotent.
--
-- Usage :
--   docker compose exec -T postgres psql -U postgres -d gestion_immobiliere < migrations/2026-10-12_mandat_mode_cumul.sql
-- ==============================================================
BEGIN;
UPDATE contrat_mandat SET mode_facturation = 'CUMUL' WHERE mode_facturation = 'ANNUEL';
COMMIT;
