/*==============================================================*/
/* Migration Flyway V9 : devis de travaux soumis au bailleur     */
/*   - devis_travaux : travaux proposes par l'agence sur une     */
/*     cour (ou une maison) sous mandat de gestion ; le bailleur */
/*     accepte, demande une renegociation ou rejette             */
/*   - depense_bien.id_devis : depense enregistree a partir      */
/*     d'un devis accepte (l'accord du bailleur est alors        */
/*     prouve par le devis, plus seulement atteste par l'agent)  */
/*==============================================================*/

CREATE SEQUENCE seq_devis_travaux START 1;

CREATE TABLE devis_travaux (
    id_devis INTEGER PRIMARY KEY DEFAULT nextval('seq_devis_travaux'),
    id_mandat INTEGER NOT NULL,
    id_cour INTEGER NOT NULL,
    id_maison INTEGER,
    libelle VARCHAR(254) NOT NULL,
    categorie VARCHAR(50) NOT NULL,
    description TEXT,
    prestataire VARCHAR(254),
    montant DECIMAL(15,2) NOT NULL,
    -- Etalement propose : nombre de reversements sur lesquels la depense serait retenue (1 = d'un coup)
    nombre_mois INTEGER NOT NULL DEFAULT 1,
    statut VARCHAR(50) NOT NULL DEFAULT 'EN_ATTENTE',
    -- Derniere reponse du bailleur : ce qu'il demande de revoir, ou pourquoi il rejette
    commentaire_bailleur VARCHAR(500),
    date_reponse TIMESTAMP(6),
    -- 1 a la creation, +1 a chaque nouvelle soumission apres une demande de renegociation
    nombre_soumissions INTEGER NOT NULL DEFAULT 1,
    motif_annulation VARCHAR(254),
    user_create INTEGER,
    user_update INTEGER,
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_devis_mandat FOREIGN KEY (id_mandat) REFERENCES contrat_mandat(id_mandat),
    CONSTRAINT fk_devis_cour FOREIGN KEY (id_cour) REFERENCES cour(id_cour),
    CONSTRAINT fk_devis_maison FOREIGN KEY (id_maison) REFERENCES maison(id_maison),
    CONSTRAINT chk_devis_montant CHECK (montant > 0),
    CONSTRAINT chk_devis_nombre_mois CHECK (nombre_mois BETWEEN 1 AND 60),
    CONSTRAINT chk_devis_statut CHECK (statut IN ('EN_ATTENTE', 'A_RENEGOCIER', 'ACCEPTE', 'REJETE', 'ANNULE', 'REALISE'))
);

CREATE INDEX idx_devis_mandat_statut ON devis_travaux (id_mandat, statut);
CREATE INDEX idx_devis_cour ON devis_travaux (id_cour);

ALTER TABLE depense_bien ADD COLUMN id_devis INTEGER;
ALTER TABLE depense_bien ADD CONSTRAINT fk_depense_devis FOREIGN KEY (id_devis) REFERENCES devis_travaux(id_devis);
-- Un devis accepte ne donne lieu qu'a une seule depense
CREATE UNIQUE INDEX uq_depense_devis ON depense_bien (id_devis) WHERE id_devis IS NOT NULL AND is_deleted = false;
