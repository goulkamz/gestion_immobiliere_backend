/*==============================================================*/
/* Migration Flyway V10 : cas limites des depenses et des devis  */
/*   - resiliation du mandat avec un solde de depenses : le      */
/*     reste devient du par le bailleur (statut A_REGLER) et se  */
/*     regle hors reversement (table depense_reglement)          */
/*   - reechelonnement du solde restant d'une depense            */
/*   - expiration des devis restes sans reponse du bailleur      */
/*==============================================================*/

-- ---- Depenses : reglement direct par le bailleur --------------
ALTER TABLE depense_bien ADD COLUMN montant_regle DECIMAL(15,2) NOT NULL DEFAULT 0;

ALTER TABLE depense_bien DROP CONSTRAINT chk_depense_impute;
ALTER TABLE depense_bien ADD CONSTRAINT chk_depense_impute
    CHECK (montant_impute >= 0 AND montant_regle >= 0 AND montant_impute + montant_regle <= montant);

ALTER TABLE depense_bien DROP CONSTRAINT chk_depense_statut;
ALTER TABLE depense_bien ADD CONSTRAINT chk_depense_statut
    CHECK (statut IN ('EN_COURS', 'A_REGLER', 'SOLDEE', 'ANNULEE'));

CREATE SEQUENCE seq_depense_reglement START 1;

CREATE TABLE depense_reglement (
    id_reglement INTEGER PRIMARY KEY DEFAULT nextval('seq_depense_reglement'),
    id_depense INTEGER NOT NULL,
    montant DECIMAL(15,2) NOT NULL,
    date_reglement DATE NOT NULL,
    mode_reglement VARCHAR(50),
    reference VARCHAR(254),
    commentaire VARCHAR(500),
    user_create INTEGER,
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_reglement_depense FOREIGN KEY (id_depense) REFERENCES depense_bien(id_depense),
    CONSTRAINT chk_reglement_montant CHECK (montant > 0)
);

CREATE INDEX idx_reglement_depense ON depense_reglement (id_depense);

-- ---- Depenses : reechelonnement -------------------------------
-- A partir du reechelonnement, le solde (montant - base_montant) est retenu sur (nombre_mois - base_mois) reversements.
-- Sans reechelonnement : base_montant = 0 et base_mois = 0, soit l'etalement d'origine.
ALTER TABLE depense_bien ADD COLUMN base_montant DECIMAL(15,2) NOT NULL DEFAULT 0;
ALTER TABLE depense_bien ADD COLUMN base_mois INTEGER NOT NULL DEFAULT 0;
ALTER TABLE depense_bien ADD COLUMN nombre_mois_initial INTEGER;
ALTER TABLE depense_bien ADD COLUMN motif_reechelonnement VARCHAR(254);
ALTER TABLE depense_bien ADD COLUMN date_reechelonnement TIMESTAMP(6);

-- ---- Devis : expiration sans reponse ---------------------------
-- Date de la soumission en cours (remise a jour a chaque nouvelle soumission) : point de depart du delai de reponse
ALTER TABLE devis_travaux ADD COLUMN date_soumission TIMESTAMP(6);
UPDATE devis_travaux SET date_soumission = COALESCE(created_at, NOW());
ALTER TABLE devis_travaux ALTER COLUMN date_soumission SET NOT NULL;
ALTER TABLE devis_travaux ALTER COLUMN date_soumission SET DEFAULT NOW();

ALTER TABLE devis_travaux DROP CONSTRAINT chk_devis_statut;
ALTER TABLE devis_travaux ADD CONSTRAINT chk_devis_statut
    CHECK (statut IN ('EN_ATTENTE', 'A_RENEGOCIER', 'ACCEPTE', 'REJETE', 'ANNULE', 'REALISE', 'EXPIRE'));

INSERT INTO parametre_systeme (cle, valeur, type_valeur, description) VALUES
    ('DELAI_REPONSE_DEVIS_JOURS', '30', 'ENTIER', 'Delai laisse au bailleur pour repondre a un devis de travaux avant son expiration')
ON CONFLICT (cle) DO NOTHING;
