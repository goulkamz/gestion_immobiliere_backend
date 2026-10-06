-- ==============================================================
-- Migration : table temoignage (GET /api/temoignages, commit f52f5bc)
-- A appliquer sur une base DEJA deployee. Ne PAS rejouer SystemImmo.sql :
-- il commence par des DROP TABLE ... CASCADE et detruirait les donnees.
-- Idempotent : peut etre rejoue sans effet si la table existe deja.
--
-- Usage :
--   psql -U postgres -d gestion_immobiliere -f migrations/2026-10-06_temoignage.sql
--   (docker : docker compose exec -T postgres psql -U postgres -d gestion_immobiliere < migrations/2026-10-06_temoignage.sql)
-- ==============================================================
BEGIN;

CREATE SEQUENCE IF NOT EXISTS seq_temoignage START 1;

CREATE TABLE IF NOT EXISTS temoignage (
    id_temoignage INTEGER PRIMARY KEY DEFAULT nextval('seq_temoignage'),
    nom_auteur VARCHAR(254) NOT NULL,
    role VARCHAR(30) NOT NULL,
    texte TEXT NOT NULL,
    note SMALLINT CHECK (note BETWEEN 1 AND 5),
    photo_url VARCHAR(512),
    date_temoignage DATE NOT NULL DEFAULT CURRENT_DATE,
    flag_actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);

COMMIT;

-- Verification :
--   \d temoignage
--   SELECT count(*) FROM temoignage;
