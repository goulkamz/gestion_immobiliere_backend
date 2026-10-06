-- ==============================================================
-- Migration : suppression de la colonne vestigiale image (SMALLINT) de cour et offre.
-- Jamais lue ni ecrite par le code ; les photos passent par la table medias
-- (entite_type COUR / OFFRE, voir 2026-10-06_medias_bien_service_offre.sql).
-- Garde-fou : refuse de supprimer si une valeur non NULL existe (donnee a examiner d'abord).
-- Idempotent : sans effet si les colonnes n'existent plus.
--
-- Usage :
--   docker compose exec -T postgres psql -U postgres -d gestion_immobiliere -v ON_ERROR_STOP=1 < migrations/2026-10-06_drop_image_cour_offre.sql
-- ==============================================================
BEGIN;

DO $$
DECLARE
    n_cour BIGINT := 0;
    n_offre BIGINT := 0;
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'cour' AND column_name = 'image') THEN
        EXECUTE 'SELECT count(*) FROM cour WHERE image IS NOT NULL' INTO n_cour;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'offre' AND column_name = 'image') THEN
        EXECUTE 'SELECT count(*) FROM offre WHERE image IS NOT NULL' INTO n_offre;
    END IF;
    IF n_cour > 0 OR n_offre > 0 THEN
        RAISE EXCEPTION 'Colonne image non vide (cour : % ligne(s), offre : % ligne(s)) : suppression annulee', n_cour, n_offre;
    END IF;
END $$;

ALTER TABLE cour  DROP COLUMN IF EXISTS image;
ALTER TABLE offre DROP COLUMN IF EXISTS image;

COMMIT;

-- Verification :
--   SELECT table_name FROM information_schema.columns WHERE column_name = 'image' AND table_name IN ('cour', 'offre');
