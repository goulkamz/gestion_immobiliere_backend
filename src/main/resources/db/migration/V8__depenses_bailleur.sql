/*==============================================================*/
/* Migration Flyway V8 : depenses a la charge du bailleur        */
/*   - depense_bien : travaux ou frais d'une cour (ou d'une      */
/*     maison) avances par l'agence, rattaches au mandat de      */
/*     gestion actif                                             */
/*   - depense_imputation : ce qui a ete retenu, reversement     */
/*     par reversement                                           */
/*   - echeance_loyer.retenue_depenses : total retenu sur un     */
/*     reversement (net = loyers - commission - retenues)        */
/*   - contrat_mandat.plafond_depense_sans_accord : montant      */
/*     jusqu'auquel l'agence engage une depense sans accord      */
/*     prealable du bailleur (NULL = aucun plafond convenu)      */
/*==============================================================*/

ALTER TABLE contrat_mandat ADD COLUMN plafond_depense_sans_accord DECIMAL(15,2);
ALTER TABLE contrat_mandat ADD CONSTRAINT chk_mandat_plafond_depense
    CHECK (plafond_depense_sans_accord IS NULL OR plafond_depense_sans_accord >= 0);

ALTER TABLE echeance_loyer ADD COLUMN retenue_depenses DECIMAL(15,2) NOT NULL DEFAULT 0;

CREATE SEQUENCE seq_depense_bien START 1;
CREATE SEQUENCE seq_depense_imputation START 1;

CREATE TABLE depense_bien (
    id_depense INTEGER PRIMARY KEY DEFAULT nextval('seq_depense_bien'),
    id_mandat INTEGER NOT NULL,
    id_cour INTEGER NOT NULL,
    id_maison INTEGER,
    libelle VARCHAR(254) NOT NULL,
    categorie VARCHAR(50) NOT NULL,
    description TEXT,
    prestataire VARCHAR(254),
    date_depense DATE NOT NULL,
    montant DECIMAL(15,2) NOT NULL,
    -- Etalement : nombre de reversements sur lesquels la depense est retenue (1 = d'un coup)
    nombre_mois INTEGER NOT NULL DEFAULT 1,
    -- Nombre de reversements deja passes sur cette depense, et total deja retenu
    mois_ecoules INTEGER NOT NULL DEFAULT 0,
    montant_impute DECIMAL(15,2) NOT NULL DEFAULT 0,
    -- Depense au-dessus du plafond du mandat : l'agent atteste l'accord du bailleur
    accord_bailleur BOOLEAN NOT NULL DEFAULT FALSE,
    statut VARCHAR(50) NOT NULL DEFAULT 'EN_COURS',
    motif_annulation VARCHAR(254),
    user_create INTEGER,
    user_update INTEGER,
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_depense_mandat FOREIGN KEY (id_mandat) REFERENCES contrat_mandat(id_mandat),
    CONSTRAINT fk_depense_cour FOREIGN KEY (id_cour) REFERENCES cour(id_cour),
    CONSTRAINT fk_depense_maison FOREIGN KEY (id_maison) REFERENCES maison(id_maison),
    CONSTRAINT chk_depense_montant CHECK (montant > 0),
    CONSTRAINT chk_depense_nombre_mois CHECK (nombre_mois BETWEEN 1 AND 60),
    CONSTRAINT chk_depense_impute CHECK (montant_impute >= 0 AND montant_impute <= montant),
    CONSTRAINT chk_depense_statut CHECK (statut IN ('EN_COURS', 'SOLDEE', 'ANNULEE'))
);

CREATE INDEX idx_depense_mandat_statut ON depense_bien (id_mandat, statut);
CREATE INDEX idx_depense_cour ON depense_bien (id_cour);

CREATE TABLE depense_imputation (
    id_imputation INTEGER PRIMARY KEY DEFAULT nextval('seq_depense_imputation'),
    id_depense INTEGER NOT NULL,
    id_echeance INTEGER NOT NULL,
    montant DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_imputation_depense FOREIGN KEY (id_depense) REFERENCES depense_bien(id_depense),
    CONSTRAINT fk_imputation_echeance FOREIGN KEY (id_echeance) REFERENCES echeance_loyer(id_echeance),
    CONSTRAINT chk_imputation_montant CHECK (montant > 0)
);

CREATE INDEX idx_imputation_depense ON depense_imputation (id_depense);
CREATE INDEX idx_imputation_echeance ON depense_imputation (id_echeance);
