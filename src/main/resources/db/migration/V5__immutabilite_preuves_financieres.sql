/*==============================================================*/
/* Migration Flyway V5 : immutabilite des preuves financieres    */
/* et du journal d'audit.                                        */
/*   - paiement, paiement_echeance, paiement_location_bien_      */
/*     service, remboursement : preuves d'argent recu ou rendu   */
/*   - journal_operation : qui a fait quoi, quand                */
/* Une ligne ne peut ni etre supprimee (ni logiquement ni        */
/* physiquement), ni voir son contenu modifie : seule la date    */
/* updated_at peut bouger. Une erreur se corrige par une         */
/* nouvelle ligne (remboursement, paiement inverse), jamais en   */
/* reecrivant l'historique. Cela vaut pour tous les comptes,     */
/* y compris le compte proprietaire.                             */
/* Maintenance exceptionnelle (DBA) : dans une transaction,      */
/*   SET LOCAL gestimmo.maintenance = 'on';                      */
/* L'application ne positionne jamais ce reglage.                */
/*==============================================================*/

CREATE OR REPLACE FUNCTION fn_preuve_immuable()
RETURNS trigger AS $$
BEGIN
    IF COALESCE(current_setting('gestimmo.maintenance', true), '') = 'on' THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        END IF;
        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Suppression interdite sur % : preuve financiere ou d''audit, elle ne se corrige que par une nouvelle ligne', TG_TABLE_NAME
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;

    IF (to_jsonb(NEW) - 'updated_at') IS DISTINCT FROM (to_jsonb(OLD) - 'updated_at') THEN
        RAISE EXCEPTION 'Modification interdite sur % : preuve financiere ou d''audit, elle ne se corrige que par une nouvelle ligne', TG_TABLE_NAME
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_immuable_paiement
    BEFORE UPDATE OR DELETE ON paiement
    FOR EACH ROW EXECUTE FUNCTION fn_preuve_immuable();

CREATE TRIGGER trg_immuable_paiement_echeance
    BEFORE UPDATE OR DELETE ON paiement_echeance
    FOR EACH ROW EXECUTE FUNCTION fn_preuve_immuable();

CREATE TRIGGER trg_immuable_paiement_location_bien_service
    BEFORE UPDATE OR DELETE ON paiement_location_bien_service
    FOR EACH ROW EXECUTE FUNCTION fn_preuve_immuable();

CREATE TRIGGER trg_immuable_remboursement
    BEFORE UPDATE OR DELETE ON remboursement
    FOR EACH ROW EXECUTE FUNCTION fn_preuve_immuable();

CREATE TRIGGER trg_immuable_journal_operation
    BEFORE UPDATE OR DELETE ON journal_operation
    FOR EACH ROW EXECUTE FUNCTION fn_preuve_immuable();
