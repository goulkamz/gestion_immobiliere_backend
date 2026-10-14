-- ==============================================================
-- Migration : ajout d'une description libre (etat des lieux, atouts...) a cour et maison.
-- Idempotent.
--
-- Usage :
--   docker compose exec -T postgres psql -U postgres -d gestion_immobiliere -v ON_ERROR_STOP=1 < migrations/2026-10-13_description_cour_maison.sql
-- ==============================================================
BEGIN;

ALTER TABLE cour   ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE maison ADD COLUMN IF NOT EXISTS description TEXT;

COMMIT;
