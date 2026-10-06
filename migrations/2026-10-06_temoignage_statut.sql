-- ==============================================================
-- Migration : temoignage.flag_actif (booleen) -> temoignage.statut (EN_ATTENTE / PUBLIE / RETIRE)
-- A appliquer APRES 2026-10-06_temoignage.sql, sur une base deja deployee.
-- Conversion : flag_actif = true -> PUBLIE ; flag_actif = false -> EN_ATTENTE
-- (les anciens "false" ne distinguaient pas "en attente" de "retire" : a requalifier
--  en RETIRE a la main si besoin, ex. UPDATE temoignage SET statut = 'RETIRE' WHERE id_temoignage IN (...)).
-- Idempotent : sans effet si la colonne statut existe deja.
--
-- Usage :
--   docker compose exec -T postgres psql -U postgres -d gestion_immobiliere < migrations/2026-10-06_temoignage_statut.sql
-- ==============================================================
BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'temoignage' AND column_name = 'statut') THEN
        ALTER TABLE temoignage ADD COLUMN statut VARCHAR(20);
        UPDATE temoignage SET statut = CASE WHEN flag_actif THEN 'PUBLIE' ELSE 'EN_ATTENTE' END;
        ALTER TABLE temoignage ALTER COLUMN statut SET NOT NULL;
        ALTER TABLE temoignage ALTER COLUMN statut SET DEFAULT 'EN_ATTENTE';
        ALTER TABLE temoignage ADD CONSTRAINT temoignage_statut_check
            CHECK (statut IN ('EN_ATTENTE', 'PUBLIE', 'RETIRE'));
        ALTER TABLE temoignage DROP COLUMN flag_actif;
    END IF;
END $$;

COMMIT;

-- Verification :
--   SELECT statut, count(*) FROM temoignage GROUP BY statut;
