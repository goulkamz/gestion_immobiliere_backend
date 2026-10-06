-- ==============================================================
-- Migration : medias pour bien_service (BIEN_SERVICE) et offre (OFFRE)
--  1. elargit la contrainte chk_medias_entite aux deux nouveaux types ;
--  2. ajoute les triggers de suppression en cascade (bien_service / offre -> medias).
-- A appliquer sur une base deja deployee, AVANT de deployer l'image backend.
-- Idempotent : peut etre rejoue sans effet (DROP ... IF EXISTS puis recreation).
-- La fonction fn_cascade_soft_delete_polymorphic existe deja (cascade_soft_delete_triggers.sql).
--
-- Usage :
--   docker compose exec -T postgres psql -U postgres -d gestion_immobiliere -v ON_ERROR_STOP=1 < migrations/2026-10-06_medias_bien_service_offre.sql
-- ==============================================================
BEGIN;

ALTER TABLE medias DROP CONSTRAINT IF EXISTS chk_medias_entite;
ALTER TABLE medias ADD CONSTRAINT chk_medias_entite CHECK (
    (entite_type = 'COUR' AND entite_id IS NOT NULL) OR
    (entite_type = 'ANNONCE' AND entite_id IS NOT NULL) OR
    (entite_type = 'MAISON' AND entite_id IS NOT NULL) OR
    (entite_type = 'BIEN_SERVICE' AND entite_id IS NOT NULL) OR
    (entite_type = 'OFFRE' AND entite_id IS NOT NULL)
);

DROP TRIGGER IF EXISTS trg_cascade_bienservice_medias ON bien_service;
CREATE TRIGGER trg_cascade_bienservice_medias
AFTER UPDATE OF is_deleted ON bien_service
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_polymorphic('medias', 'entite_id', 'entite_type', 'BIEN_SERVICE', 'id_bien_service');

DROP TRIGGER IF EXISTS trg_cascade_offre_medias ON offre;
CREATE TRIGGER trg_cascade_offre_medias
AFTER UPDATE OF is_deleted ON offre
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_polymorphic('medias', 'entite_id', 'entite_type', 'OFFRE', 'id_offre');

COMMIT;

-- Verification :
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'chk_medias_entite';
--   SELECT tgname FROM pg_trigger WHERE tgname LIKE 'trg_cascade_%medias';
