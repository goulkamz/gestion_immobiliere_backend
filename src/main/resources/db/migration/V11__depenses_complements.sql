/*==============================================================*/
/* Migration Flyway V11 : complements des depenses et des devis  */
/*   - annulation d'un reglement saisi par erreur (le reglement  */
/*     reste en base, marque annule, avec son motif)             */
/*   - abandon de creance par l'admin sur une depense due        */
/*   - accord du bailleur atteste par l'agent : motif obligatoire */
/*   - devis : accord donne hors plateforme (enregistre par      */
/*     l'agent, conteste par le bailleur si besoin)              */
/*   - rappel de l'agent pour un devis accepte jamais realise    */
/*==============================================================*/

-- ---- Reglements : annulation ------------------------------------
ALTER TABLE depense_reglement ADD COLUMN annule BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE depense_reglement ADD COLUMN motif_annulation VARCHAR(254);
ALTER TABLE depense_reglement ADD COLUMN date_annulation TIMESTAMP(6);
ALTER TABLE depense_reglement ADD COLUMN user_annulation INTEGER;

-- ---- Depenses : abandon de creance ------------------------------
-- montant_abandonne = reste du que l'agence renonce a reclamer ; la depense est alors SOLDEE
ALTER TABLE depense_bien ADD COLUMN montant_abandonne DECIMAL(15,2) NOT NULL DEFAULT 0;
ALTER TABLE depense_bien ADD COLUMN motif_abandon VARCHAR(254);
ALTER TABLE depense_bien ADD COLUMN date_abandon TIMESTAMP(6);
ALTER TABLE depense_bien ADD COLUMN user_abandon INTEGER;

ALTER TABLE depense_bien DROP CONSTRAINT chk_depense_impute;
ALTER TABLE depense_bien ADD CONSTRAINT chk_depense_impute
    CHECK (montant_impute >= 0 AND montant_regle >= 0 AND montant_abandonne >= 0
           AND montant_impute + montant_regle + montant_abandonne <= montant);

-- ---- Depenses : accord du bailleur atteste -----------------------
-- Qui a donne l'accord, quand, par quel moyen (depense au-dessus du plafond, ou issue d'un devis accepte hors plateforme)
ALTER TABLE depense_bien ADD COLUMN accord_motif VARCHAR(500);

-- ---- Devis : accord donne hors plateforme ------------------------
ALTER TABLE devis_travaux ADD COLUMN accord_hors_plateforme BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE devis_travaux ADD COLUMN accord_motif VARCHAR(500);
ALTER TABLE devis_travaux ADD COLUMN accord_date DATE;
ALTER TABLE devis_travaux ADD COLUMN accord_user INTEGER;

INSERT INTO parametre_systeme (cle, valeur, type_valeur, description) VALUES
    ('DELAI_RAPPEL_DEVIS_ACCEPTE_JOURS', '30', 'ENTIER', 'Delai apres lequel l''agent est rappele qu''un devis accepte par le bailleur n''a toujours pas donne lieu a une depense (rappel repete a chaque periode ecoulee)')
ON CONFLICT (cle) DO NOTHING;
