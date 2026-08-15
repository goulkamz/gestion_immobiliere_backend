/*==============================================================*/
/* Migration : Cascade Soft Delete / Restore (triggers)        */
/* SGBD      : PostgreSQL                                       */
/* Prérequis : V1__init_schema.sql (SystemImmo.sql) déjà exécuté */
/*==============================================================*/

-- ==============================================================
-- Nettoyage des objets existants (permet le re-run de ce script)
-- ==============================================================

DROP TRIGGER IF EXISTS trg_cascade_pays_ville ON pays;
DROP TRIGGER IF EXISTS trg_cascade_ville_secteur ON ville;
DROP TRIGGER IF EXISTS trg_cascade_secteur_cour ON secteur;
DROP TRIGGER IF EXISTS trg_cascade_secteur_bienservice ON secteur;
DROP TRIGGER IF EXISTS trg_cascade_categorie_bienservice ON categorie_bien_service;
DROP TRIGGER IF EXISTS trg_cascade_cour_maison ON cour;
DROP TRIGGER IF EXISTS trg_cascade_cour_mandat ON cour;
DROP TRIGGER IF EXISTS trg_cascade_cour_medias ON cour;
DROP TRIGGER IF EXISTS trg_cascade_maison_contralocation ON maison;
DROP TRIGGER IF EXISTS trg_cascade_maison_reservation ON maison;
DROP TRIGGER IF EXISTS trg_cascade_maison_medias ON maison;
DROP TRIGGER IF EXISTS trg_cascade_bienservice_location ON bien_service;
DROP TRIGGER IF EXISTS trg_cascade_mandat_echeance ON contrat_mandat;
DROP TRIGGER IF EXISTS trg_cascade_contralocation_echeance ON contra_location;
DROP TRIGGER IF EXISTS trg_cascade_annonce_medias ON annonce;

DROP FUNCTION IF EXISTS fn_cascade_soft_delete_generic() CASCADE;
DROP FUNCTION IF EXISTS fn_cascade_soft_delete_polymorphic() CASCADE;

-- ==============================================================
-- Fonction générique : relation avec FK classique
-- Propage la valeur is_deleted du parent (NEW) vers la table
-- enfant, dans les deux sens (delete : false->true,
-- restore : true->false), en évitant les updates inutiles.
-- Arguments (TG_ARGV) :
--   [0] child_table  : nom de la table enfant
--   [1] fk_column    : colonne FK dans la table enfant
--   [2] pk_column    : colonne PK dans la table parente (NEW)
-- ==============================================================
CREATE OR REPLACE FUNCTION fn_cascade_soft_delete_generic()
RETURNS TRIGGER AS $$
DECLARE
    child_table TEXT := TG_ARGV[0];
    fk_column   TEXT := TG_ARGV[1];
    pk_column   TEXT := TG_ARGV[2];
    pk_value    INTEGER;
BEGIN
    EXECUTE format('SELECT ($1).%I', pk_column) INTO pk_value USING NEW;

    EXECUTE format(
        'UPDATE %I SET is_deleted = $1 WHERE %I = $2 AND is_deleted IS DISTINCT FROM $1',
        child_table, fk_column
    ) USING NEW.is_deleted, pk_value;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ==============================================================
-- Fonction générique : relation polymorphe (entite_type + entite_id)
-- Utilisée pour medias et echeance_loyer.
-- Arguments (TG_ARGV) :
--   [0] child_table  : nom de la table enfant
--   [1] fk_column    : colonne id polymorphe (entite_id / entite_echeance_id)
--   [2] type_column  : colonne type polymorphe (entite_type / entite_echeance_type)
--   [3] type_value   : valeur du type à filtrer ('COUR', 'MAISON', 'MANDAT', ...)
--   [4] pk_column    : colonne PK dans la table parente (NEW)
-- ==============================================================
CREATE OR REPLACE FUNCTION fn_cascade_soft_delete_polymorphic()
RETURNS TRIGGER AS $$
DECLARE
    child_table TEXT := TG_ARGV[0];
    fk_column   TEXT := TG_ARGV[1];
    type_column TEXT := TG_ARGV[2];
    type_value  TEXT := TG_ARGV[3];
    pk_column   TEXT := TG_ARGV[4];
    pk_value    INTEGER;
BEGIN
    EXECUTE format('SELECT ($1).%I', pk_column) INTO pk_value USING NEW;

    EXECUTE format(
        'UPDATE %I SET is_deleted = $1 WHERE %I = $2 AND %I = $3 AND is_deleted IS DISTINCT FROM $1',
        child_table, fk_column, type_column
    ) USING NEW.is_deleted, pk_value, type_value;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ==============================================================
-- Triggers : hiérarchie géographique
-- pays -> ville -> secteur
-- ==============================================================

CREATE TRIGGER trg_cascade_pays_ville
AFTER UPDATE OF is_deleted ON pays
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_generic('ville', 'id_pays', 'id_pays');

CREATE TRIGGER trg_cascade_ville_secteur
AFTER UPDATE OF is_deleted ON ville
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_generic('secteur', 'id_ville', 'id_ville');

-- ==============================================================
-- Triggers : secteur -> cour / bien_service
-- ==============================================================

CREATE TRIGGER trg_cascade_secteur_cour
AFTER UPDATE OF is_deleted ON secteur
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_generic('cour', 'id_secteur', 'id_secteur');

CREATE TRIGGER trg_cascade_secteur_bienservice
AFTER UPDATE OF is_deleted ON secteur
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_generic('bien_service', 'id_secteur', 'id_secteur');

-- ==============================================================
-- Trigger : categorie_bien_service -> bien_service
-- ==============================================================

CREATE TRIGGER trg_cascade_categorie_bienservice
AFTER UPDATE OF is_deleted ON categorie_bien_service
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_generic('bien_service', 'id_categorie', 'id_categorie');

-- ==============================================================
-- Triggers : cour -> maison / contrat_mandat / medias(COUR)
-- ==============================================================

CREATE TRIGGER trg_cascade_cour_maison
AFTER UPDATE OF is_deleted ON cour
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_generic('maison', 'id_cour', 'id_cour');

CREATE TRIGGER trg_cascade_cour_mandat
AFTER UPDATE OF is_deleted ON cour
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_generic('contrat_mandat', 'id_cour', 'id_cour');

CREATE TRIGGER trg_cascade_cour_medias
AFTER UPDATE OF is_deleted ON cour
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_polymorphic('medias', 'entite_id', 'entite_type', 'COUR', 'id_cour');

-- ==============================================================
-- Triggers : maison -> contra_location / reservation_maison / medias(MAISON)
-- ==============================================================

CREATE TRIGGER trg_cascade_maison_contralocation
AFTER UPDATE OF is_deleted ON maison
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_generic('contra_location', 'id_maison', 'id_maison');

CREATE TRIGGER trg_cascade_maison_reservation
AFTER UPDATE OF is_deleted ON maison
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_generic('reservation_maison', 'id_maison', 'id_maison');

CREATE TRIGGER trg_cascade_maison_medias
AFTER UPDATE OF is_deleted ON maison
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_polymorphic('medias', 'entite_id', 'entite_type', 'MAISON', 'id_maison');

-- ==============================================================
-- Trigger : bien_service -> location_bien_service
-- ==============================================================

CREATE TRIGGER trg_cascade_bienservice_location
AFTER UPDATE OF is_deleted ON bien_service
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_generic('location_bien_service', 'id_bien_service', 'id_bien_service');

-- ==============================================================
-- Triggers : échéances (relation polymorphe MANDAT / LOCATION)
-- ==============================================================

CREATE TRIGGER trg_cascade_mandat_echeance
AFTER UPDATE OF is_deleted ON contrat_mandat
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_polymorphic('echeance_loyer', 'entite_echeance_id', 'entite_echeance_type', 'MANDAT', 'id_mandat');

CREATE TRIGGER trg_cascade_contralocation_echeance
AFTER UPDATE OF is_deleted ON contra_location
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_polymorphic('echeance_loyer', 'entite_echeance_id', 'entite_echeance_type', 'LOCATION', 'id_contra_location');

-- ==============================================================
-- Trigger : annonce -> medias(ANNONCE)
-- ==============================================================

CREATE TRIGGER trg_cascade_annonce_medias
AFTER UPDATE OF is_deleted ON annonce
FOR EACH ROW WHEN (OLD.is_deleted IS DISTINCT FROM NEW.is_deleted)
EXECUTE FUNCTION fn_cascade_soft_delete_polymorphic('medias', 'entite_id', 'entite_type', 'ANNONCE', 'id_annonce');

-- ==============================================================
-- Tables volontairement EXCLUES de la cascade (décision métier) :
--   - users            : désactiver un compte ne doit pas supprimer
--                         ses biens/contrats liés
--   - role              : trop sensible pour être automatisé
--   - paiement          : preuve financière, ne doit jamais disparaître
--   - paiement_echeance : table de liaison de preuve financière
--   - journal_operation : preuve d'audit, immuable par nature
-- ==============================================================

-- ==============================================================
-- Vérification
-- ==============================================================
SELECT 'Triggers de cascade soft delete/restore installés avec succès !' as status;
SELECT COUNT(*) as triggers_crees
FROM information_schema.triggers
WHERE trigger_name LIKE 'trg_cascade_%';