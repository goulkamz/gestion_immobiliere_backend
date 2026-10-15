-- Avenants : trace des modifications d'un contrat actif (location ou mandat).
-- Le contrat d'origine reste fige ; chaque modification cree un avenant numerote
-- dont les lignes listent champ, ancienne valeur et nouvelle valeur.
CREATE SEQUENCE IF NOT EXISTS seq_avenant START 1;
CREATE SEQUENCE IF NOT EXISTS seq_avenant_ligne START 1;

CREATE TABLE IF NOT EXISTS avenant (
    id_avenant INTEGER PRIMARY KEY DEFAULT nextval('seq_avenant'),
    id_contrat_location INTEGER,
    id_mandat INTEGER,
    numero_ordre INTEGER NOT NULL,
    date_effet DATE NOT NULL,
    motif TEXT NOT NULL,
    user_create INTEGER,
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_avenant_location FOREIGN KEY (id_contrat_location) REFERENCES contra_location(id_contra_location),
    CONSTRAINT fk_avenant_mandat FOREIGN KEY (id_mandat) REFERENCES contrat_mandat(id_mandat),
    -- Un avenant porte sur exactement un contrat : location OU mandat
    CONSTRAINT ck_avenant_un_contrat CHECK ((id_contrat_location IS NULL) <> (id_mandat IS NULL))
);

CREATE TABLE IF NOT EXISTS avenant_ligne (
    id_ligne INTEGER PRIMARY KEY DEFAULT nextval('seq_avenant_ligne'),
    id_avenant INTEGER NOT NULL,
    champ VARCHAR(50) NOT NULL,
    ancienne_valeur VARCHAR(255),
    nouvelle_valeur VARCHAR(255),
    CONSTRAINT fk_avenant_ligne_avenant FOREIGN KEY (id_avenant) REFERENCES avenant(id_avenant) ON DELETE CASCADE
);

-- Numerotation sans doublon par contrat (garde-fou en cas de modifications simultanees)
CREATE UNIQUE INDEX IF NOT EXISTS uq_avenant_location_numero
    ON avenant (id_contrat_location, numero_ordre) WHERE id_contrat_location IS NOT NULL AND is_deleted IS NOT TRUE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_avenant_mandat_numero
    ON avenant (id_mandat, numero_ordre) WHERE id_mandat IS NOT NULL AND is_deleted IS NOT TRUE;
CREATE INDEX IF NOT EXISTS idx_avenant_ligne_avenant ON avenant_ligne (id_avenant);
