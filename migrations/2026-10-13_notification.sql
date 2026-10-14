-- ==============================================================
-- Migration : table notification (notifications in-app, tranche 1)
-- A appliquer sur une base DEJA deployee. Ne PAS rejouer SystemImmo.sql :
-- il commence par des DROP TABLE ... CASCADE et detruirait les donnees.
-- Idempotent : peut etre rejoue sans effet si la table existe deja.
--
-- Usage :
--   docker compose exec -T postgres psql -U postgres -d gestion_immobiliere < migrations/2026-10-13_notification.sql
-- ==============================================================
BEGIN;

CREATE SEQUENCE IF NOT EXISTS seq_notification START 1;

CREATE TABLE IF NOT EXISTS notification (
    id_notification INTEGER PRIMARY KEY DEFAULT nextval('seq_notification'),
    id_user INTEGER NOT NULL REFERENCES users(id_user),
    type VARCHAR(50) NOT NULL,
    titre VARCHAR(200) NOT NULL,
    message TEXT,
    lien VARCHAR(255),
    cle_alerte VARCHAR(150),
    lue BOOLEAN NOT NULL DEFAULT FALSE,
    date_lecture TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_notification_user ON notification (id_user, lue, created_at DESC);
-- Anti-doublons : une meme alerte (ex. LOCATION_ECHEANCE:11:J-2) n'existe qu'une fois par utilisateur
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_cle ON notification (id_user, cle_alerte) WHERE cle_alerte IS NOT NULL;

COMMIT;
