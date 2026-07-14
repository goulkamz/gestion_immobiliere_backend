/*==============================================================*/
/* SGBD : PostgreSQL                                            */
/* Date : 12/07/2026 (mise à jour)                              */
/* Version snake_case - Compatible avec PostgreSQL et JPA      */
/*==============================================================*/

-- ==============================================================
-- Nettoyage des objets existants
-- ==============================================================

DROP TYPE IF EXISTS type_echeance_contrat CASCADE;
DROP TYPE IF EXISTS type_entite_media CASCADE;

DROP TABLE IF EXISTS paiement_echeance CASCADE;
DROP TABLE IF EXISTS medias CASCADE;
DROP TABLE IF EXISTS echeance_loyer CASCADE;
DROP TABLE IF EXISTS location_bien_service CASCADE;
DROP TABLE IF EXISTS reservation_maison CASCADE;
DROP TABLE IF EXISTS contra_location CASCADE;
DROP TABLE IF EXISTS contrat_mandat CASCADE;
DROP TABLE IF EXISTS journal_operation CASCADE;
DROP TABLE IF EXISTS maison CASCADE;
DROP TABLE IF EXISTS cour CASCADE;
DROP TABLE IF EXISTS bien_service CASCADE;
DROP TABLE IF EXISTS demande CASCADE;
DROP TABLE IF EXISTS offre CASCADE;
DROP TABLE IF EXISTS contact CASCADE;
DROP TABLE IF EXISTS annonce CASCADE;
DROP TABLE IF EXISTS categorie_bien_service CASCADE;
DROP TABLE IF EXISTS secteur CASCADE;
DROP TABLE IF EXISTS ville CASCADE;
DROP TABLE IF EXISTS pays CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS role CASCADE;
DROP TABLE IF EXISTS paiement CASCADE;
DROP TABLE IF EXISTS pending_registration CASCADE;
DROP TABLE IF EXISTS password_reset_token CASCADE;

DROP SEQUENCE IF EXISTS seq_annonce CASCADE;
DROP SEQUENCE IF EXISTS seq_bien_service CASCADE;
DROP SEQUENCE IF EXISTS seq_categorie CASCADE;
DROP SEQUENCE IF EXISTS seq_contact CASCADE;
DROP SEQUENCE IF EXISTS seq_contra_location CASCADE;
DROP SEQUENCE IF EXISTS seq_contrat_mandat CASCADE;
DROP SEQUENCE IF EXISTS seq_cour CASCADE;
DROP SEQUENCE IF EXISTS seq_demande CASCADE;
DROP SEQUENCE IF EXISTS seq_echeance CASCADE;
DROP SEQUENCE IF EXISTS seq_journal CASCADE;
DROP SEQUENCE IF EXISTS seq_location_service CASCADE;
DROP SEQUENCE IF EXISTS seq_maison CASCADE;
DROP SEQUENCE IF EXISTS seq_media CASCADE;
DROP SEQUENCE IF EXISTS seq_offre CASCADE;
DROP SEQUENCE IF EXISTS seq_paiement CASCADE;
DROP SEQUENCE IF EXISTS seq_pays CASCADE;
DROP SEQUENCE IF EXISTS seq_reservation CASCADE;
DROP SEQUENCE IF EXISTS seq_role CASCADE;
DROP SEQUENCE IF EXISTS seq_secteur CASCADE;
DROP SEQUENCE IF EXISTS seq_users CASCADE;
DROP SEQUENCE IF EXISTS seq_ville CASCADE;
DROP SEQUENCE IF EXISTS seq_pending_registration CASCADE;
DROP SEQUENCE IF EXISTS seq_password_reset_token CASCADE;
DROP SEQUENCE IF EXISTS seq_paiement_echeance CASCADE;

-- ==============================================================
-- Séquences
-- ==============================================================
CREATE SEQUENCE seq_annonce START 1;
CREATE SEQUENCE seq_bien_service START 1;
CREATE SEQUENCE seq_categorie START 1;
CREATE SEQUENCE seq_contact START 1;
CREATE SEQUENCE seq_contra_location START 1;
CREATE SEQUENCE seq_contrat_mandat START 1;
CREATE SEQUENCE seq_cour START 1;
CREATE SEQUENCE seq_demande START 1;
CREATE SEQUENCE seq_echeance START 1;
CREATE SEQUENCE seq_journal START 1;
CREATE SEQUENCE seq_location_service START 1;
CREATE SEQUENCE seq_maison START 1;
CREATE SEQUENCE seq_media START 1;
CREATE SEQUENCE seq_offre START 1;
CREATE SEQUENCE seq_paiement START 1;
CREATE SEQUENCE seq_pays START 1;
CREATE SEQUENCE seq_reservation START 1;
CREATE SEQUENCE seq_role START 1;
CREATE SEQUENCE seq_secteur START 1;
CREATE SEQUENCE seq_users START 1;
CREATE SEQUENCE seq_ville START 1;
CREATE SEQUENCE seq_pending_registration START 1;
CREATE SEQUENCE seq_password_reset_token START 1;
CREATE SEQUENCE seq_paiement_echeance START 1;

-- ==============================================================
-- Types ENUM
-- ==============================================================
CREATE TYPE type_echeance_contrat AS ENUM ('MANDAT', 'LOCATION');
CREATE TYPE type_entite_media AS ENUM ('COURS', 'ANNONCE', 'MAISON');

-- ==============================================================
-- Table: role
-- ==============================================================
CREATE TABLE role (
    id_role INTEGER PRIMARY KEY DEFAULT nextval('seq_role'),
    libelle_role VARCHAR(254) UNIQUE NOT NULL,
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);

-- ==============================================================
-- Table: users
-- date_naissance reste DATE (décision confirmée : jamais d'heure)
-- date_create / date_last_login migrés en TIMESTAMP
-- ==============================================================
CREATE TABLE users (
    id_user INTEGER PRIMARY KEY DEFAULT nextval('seq_users'),
    id_role INTEGER NOT NULL,
    nom VARCHAR(254),
    prenom VARCHAR(254),
    sexe CHAR(1),
    email VARCHAR(254) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(254) NOT NULL,
    date_naissance DATE,
    telephone VARCHAR(254) UNIQUE,
    telephone1 VARCHAR(254) UNIQUE,
    flag_actif BOOLEAN,
    date_create TIMESTAMP(6),
    date_last_login TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_users_role FOREIGN KEY (id_role) REFERENCES role(id_role)
);

-- ==============================================================
-- Table: password_reset_token
-- ==============================================================
CREATE TABLE password_reset_token (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    token VARCHAR(255) NOT NULL UNIQUE,
    creation TIMESTAMP NOT NULL,
    expiration TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    attempt_count INTEGER DEFAULT 0
);

-- ==============================================================
-- Table: pending_registration
-- date_naissance reste DATE (même donnée que users.date_naissance)
-- ==============================================================
CREATE TABLE pending_registration (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(120) NOT NULL,
    nom VARCHAR(254) NOT NULL,
    prenom VARCHAR(254) NOT NULL,
    sexe CHAR(1),
    telephone VARCHAR(254),
    telephone1 VARCHAR(254),
    id_role INTEGER NOT NULL,
    flag_actif BOOLEAN,
    date_naissance DATE,
    code VARCHAR(10) NOT NULL UNIQUE,
    creation TIMESTAMP NOT NULL,
    expiration TIMESTAMP NOT NULL,
    tentative_envoi INTEGER DEFAULT 0
);

-- ==============================================================
-- Table: pays
-- ==============================================================
CREATE TABLE pays (
    id_pays INTEGER PRIMARY KEY DEFAULT nextval('seq_pays'),
    code_pays VARCHAR(254),
    nom_pays VARCHAR(254),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);

-- ==============================================================
-- Table: ville
-- ==============================================================
CREATE TABLE ville (
    id_ville INTEGER PRIMARY KEY DEFAULT nextval('seq_ville'),
    id_pays INTEGER NOT NULL,
    code_ville VARCHAR(254),
    nom_ville VARCHAR(254),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_ville_pays FOREIGN KEY (id_pays) REFERENCES pays(id_pays)
);

-- ==============================================================
-- Table: secteur
-- ==============================================================
CREATE TABLE secteur (
    id_secteur INTEGER PRIMARY KEY DEFAULT nextval('seq_secteur'),
    id_ville INTEGER NOT NULL,
    code_secteur VARCHAR(254),
    nom_secteur VARCHAR(254),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_secteur_ville FOREIGN KEY (id_ville) REFERENCES ville(id_ville)
);

-- ==============================================================
-- Table: categorie_bien_service
-- date_create / date_update migrés en TIMESTAMP
-- ==============================================================
CREATE TABLE categorie_bien_service (
    id_categorie INTEGER PRIMARY KEY DEFAULT nextval('seq_categorie'),
    libelle VARCHAR(254),
    description VARCHAR(254),
    user_create INTEGER,
    user_update INTEGER,
    date_create TIMESTAMP(6),
    date_update TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);

-- ==============================================================
-- Table: bien_service
-- date_create / date_update migrés en TIMESTAMP
-- ==============================================================
CREATE TABLE bien_service (
    id_bien_service INTEGER PRIMARY KEY DEFAULT nextval('seq_bien_service'),
    id_secteur INTEGER NOT NULL,
    id_categorie INTEGER NOT NULL,
    id_user INTEGER NOT NULL,
    libelle VARCHAR(254),
    description VARCHAR(254),
    prix_journalier FLOAT8,
    prix_mensuel FLOAT8,
    disponibilite VARCHAR(254),
    user_create INTEGER,
    user_update INTEGER,
    date_create TIMESTAMP(6),
    date_update TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_bienservice_secteur FOREIGN KEY (id_secteur) REFERENCES secteur(id_secteur),
    CONSTRAINT fk_bienservice_categorie FOREIGN KEY (id_categorie) REFERENCES categorie_bien_service(id_categorie),
    CONSTRAINT fk_bienservice_user FOREIGN KEY (id_user) REFERENCES users(id_user)
);

-- ==============================================================
-- Table: cour
-- date_create / date_update migrés en TIMESTAMP
-- ==============================================================
CREATE TABLE cour (
    id_cour INTEGER PRIMARY KEY DEFAULT nextval('seq_cour'),
    id_secteur INTEGER NOT NULL,
    id_user INTEGER NOT NULL,
    reference_cours VARCHAR(254),
    lot_cours VARCHAR(254),
    numero_porte INTEGER,
    image SMALLINT,
    user_create INTEGER,
    user_update INTEGER,
    date_create TIMESTAMP(6),
    date_update TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_cour_secteur FOREIGN KEY (id_secteur) REFERENCES secteur(id_secteur),
    CONSTRAINT fk_cour_user FOREIGN KEY (id_user) REFERENCES users(id_user)
);

-- ==============================================================
-- Table: maison
-- date_create / date_update migrés en TIMESTAMP
-- ==============================================================
CREATE TABLE maison (
    id_maison INTEGER PRIMARY KEY DEFAULT nextval('seq_maison'),
    id_cour INTEGER NOT NULL,
    type_maison VARCHAR(254),
    nom_commun_maison VARCHAR(254),
    nombre_piece INTEGER,
    loyer FLOAT8,
    caution FLOAT8,
    nombre_mois_caution INTEGER,
    statut VARCHAR(254),
    user_create INTEGER,
    user_update INTEGER,
    date_create TIMESTAMP(6),
    date_update TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_maison_cour FOREIGN KEY (id_cour) REFERENCES cour(id_cour)
);

-- ==============================================================
-- Table: annonce
-- date_publication / date_expiration migrés en TIMESTAMP
-- ==============================================================
CREATE TABLE annonce (
    id_annonce INTEGER PRIMARY KEY DEFAULT nextval('seq_annonce'),
    titre VARCHAR(254),
    description VARCHAR(254),
    type_annonce VARCHAR(254),
    date_publication TIMESTAMP(6),
    date_expiration TIMESTAMP(6),
    statut VARCHAR(254),
    prix FLOAT8,
    localisation VARCHAR(254),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);

-- ==============================================================
-- Table: contact
-- date_envoi migré en TIMESTAMP
-- ==============================================================
CREATE TABLE contact (
    id_contact INTEGER PRIMARY KEY DEFAULT nextval('seq_contact'),
    nom_complet VARCHAR(254),
    email VARCHAR(254),
    telephone VARCHAR(254),
    sujet VARCHAR(254),
    message VARCHAR(254),
    date_envoi TIMESTAMP(6),
    statut VARCHAR(254),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);

-- ==============================================================
-- Table: contra_location
-- date_entree / date_sortie / date_create / date_update -> TIMESTAMP
-- depot_garantie : DATE -> FLOAT8 (anomalie corrigée, c'est un montant)
-- ==============================================================
CREATE TABLE contra_location (
    id_contra_location INTEGER PRIMARY KEY DEFAULT nextval('seq_contra_location'),
    id_user INTEGER NOT NULL,
    id_maison INTEGER NOT NULL,
    date_entree TIMESTAMP(6),
    date_sortie TIMESTAMP(6),
    montant_loyer FLOAT8,
    statut VARCHAR(254),
    type_contrat VARCHAR(254),
    depot_garantie FLOAT8,
    etat_des_lieux_entree VARCHAR(254),
    etat_des_lieux_sortie VARCHAR(254),
    user_create INTEGER,
    user_update INTEGER,
    date_create TIMESTAMP(6),
    date_update TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_contralocation_user FOREIGN KEY (id_user) REFERENCES users(id_user),
    CONSTRAINT fk_contralocation_maison FOREIGN KEY (id_maison) REFERENCES maison(id_maison)
);

-- ==============================================================
-- Table: contrat_mandat
-- date_debut / date_fin / date_resiliation migrés en TIMESTAMP
-- ==============================================================
CREATE TABLE contrat_mandat (
    id_mandat INTEGER PRIMARY KEY DEFAULT nextval('seq_contrat_mandat'),
    id_cour INTEGER NOT NULL,
    id_user INTEGER NOT NULL,
    date_debut TIMESTAMP(6),
    date_fin TIMESTAMP(6),
    type_mandat VARCHAR(254),
    commission NUMERIC,
    mode_facturation VARCHAR(254),
    date_resiliation TIMESTAMP(6),
    motif_resiliation VARCHAR(254),
    statut VARCHAR(254),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_contratmandat_cour FOREIGN KEY (id_cour) REFERENCES cour(id_cour),
    CONSTRAINT fk_contratmandat_user FOREIGN KEY (id_user) REFERENCES users(id_user)
);

-- ==============================================================
-- Table: demande
-- date_demande migré en TIMESTAMP
-- ==============================================================
CREATE TABLE demande (
    id_demande INTEGER PRIMARY KEY DEFAULT nextval('seq_demande'),
    nom_complet VARCHAR(254),
    email VARCHAR(254),
    telephone VARCHAR(254),
    type_bien VARCHAR(254),
    localisation_souhaite VARCHAR(254),
    budget_max FLOAT8,
    description VARCHAR(254),
    date_demande TIMESTAMP(6),
    statut VARCHAR(254),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);

-- ==============================================================
-- Table: offre
-- date_offre migré en TIMESTAMP
-- ==============================================================
CREATE TABLE offre (
    id_offre INTEGER PRIMARY KEY DEFAULT nextval('seq_offre'),
    nom_complet VARCHAR(254),
    email VARCHAR(254),
    telephone VARCHAR(254),
    type_offre VARCHAR(254),
    titre VARCHAR(254),
    description VARCHAR(254),
    adresse VARCHAR(254),
    date_offre TIMESTAMP(6),
    statut VARCHAR(254),
    image SMALLINT,
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);

-- ==============================================================
-- Table: paiement
-- date_paiement migré en TIMESTAMP
-- ==============================================================
CREATE TABLE paiement (
    id_paiement INTEGER PRIMARY KEY DEFAULT nextval('seq_paiement'),
    date_paiement TIMESTAMP(6),
    montant_paiement FLOAT8,
    mode_paiement VARCHAR(254),
    reference_paiement VARCHAR(254),
    user_create INTEGER,
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);

-- ==============================================================
-- Table: echeance_loyer
-- date_echeance reste DATE (décision confirmée)
-- date_create / date_update migrés en TIMESTAMP
-- ==============================================================
CREATE TABLE echeance_loyer (
    id_echeance INTEGER PRIMARY KEY DEFAULT nextval('seq_echeance'),
    entite_echeance_type type_echeance_contrat,
    entite_echeance_id INTEGER NOT NULL,
    date_echeance DATE,
    montant_du FLOAT8,
    montant_paye FLOAT8,
    statut VARCHAR(254),
    user_create INTEGER,
    user_update INTEGER,
    date_create TIMESTAMP(6),
    date_update TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT chk_echeance_entite CHECK (
        (entite_echeance_type = 'MANDAT' AND entite_echeance_id IS NOT NULL) OR
        (entite_echeance_type = 'LOCATION' AND entite_echeance_id IS NOT NULL)
    )
);

-- ==============================================================
-- Table: journal_operation
-- date_action migré en TIMESTAMP
-- ancien_contenu / nouveau_contenu : CHAR(254) -> TEXT (diff JSON trop long sinon)
-- ==============================================================
CREATE TABLE journal_operation (
    id_journal INTEGER PRIMARY KEY DEFAULT nextval('seq_journal'),
    id_user INTEGER ,
    action VARCHAR(254),
    entite VARCHAR(254),
    ligne_entite INTEGER,
    description VARCHAR(254),
    date_action TIMESTAMP(6),
    ancien_contenu JSONB,
    nouveau_contenu JSONB,
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_journal_user FOREIGN KEY (id_user) REFERENCES users(id_user)
);

-- ==============================================================
-- Table: location_bien_service
-- date_debut / date_fin / date_create / date_update migrés en TIMESTAMP
-- ==============================================================
CREATE TABLE location_bien_service (
    id_location_bien_service INTEGER PRIMARY KEY DEFAULT nextval('seq_location_service'),
    id_user INTEGER NOT NULL,
    id_paiement INTEGER NOT NULL,
    id_bien_service INTEGER NOT NULL,
    destination VARCHAR(254),
    date_debut TIMESTAMP(6),
    date_fin TIMESTAMP(6),
    duree INTEGER,
    montant_total FLOAT8,
    statut VARCHAR(254),
    user_create INTEGER,
    user_update INTEGER,
    date_create TIMESTAMP(6),
    date_update TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_location_user FOREIGN KEY (id_user) REFERENCES users(id_user),
    CONSTRAINT fk_location_paiement FOREIGN KEY (id_paiement) REFERENCES paiement(id_paiement),
    CONSTRAINT fk_location_bienservice FOREIGN KEY (id_bien_service) REFERENCES bien_service(id_bien_service)
);

-- ==============================================================
-- Table: medias
-- date_upload migré en TIMESTAMP
-- Ajout media_path_thumbnail (module Médias, miniatures)
-- ==============================================================
CREATE TABLE medias (
    id_media INTEGER PRIMARY KEY DEFAULT nextval('seq_media'),
    entite_type type_entite_media,
    entite_id INTEGER,
    type_media VARCHAR(254),
    media_path VARCHAR(254) NOT NULL,
    media_path_thumbnail VARCHAR(254),
    is_principal BOOLEAN DEFAULT FALSE,
    ordre SMALLINT DEFAULT 0,
    date_upload TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT chk_medias_entite CHECK (
        (entite_type = 'COURS' AND entite_id IS NOT NULL) OR
        (entite_type = 'ANNONCE' AND entite_id IS NOT NULL) OR
        (entite_type = 'MAISON' AND entite_id IS NOT NULL)
    )
);

-- ==============================================================
-- Table: reservation_maison
-- date_debut / date_fin migrés en TIMESTAMP
-- Ajout statut (cycle de vie F21 : EN_ATTENTE, CONFIRMEE, ANNULEE, CONVERTIE)
-- ==============================================================
CREATE TABLE reservation_maison (
    id_reservation INTEGER PRIMARY KEY DEFAULT nextval('seq_reservation'),
    id_user INTEGER NOT NULL,
    id_maison INTEGER NOT NULL,
    date_debut TIMESTAMP(6),
    date_fin TIMESTAMP(6),
    statut VARCHAR(254) DEFAULT 'EN_ATTENTE',
    created_at TIMESTAMP(6) DEFAULT NOW(),
    updated_at TIMESTAMP(6) DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_reservation_user FOREIGN KEY (id_user) REFERENCES users(id_user),
    CONSTRAINT fk_reservation_maison FOREIGN KEY (id_maison) REFERENCES maison(id_maison)
);

-- ==============================================================
-- Table: paiement_echeance (liaison)
-- ==============================================================
CREATE TABLE paiement_echeance (
    id_echeance INTEGER NOT NULL,
    id_paiement INTEGER NOT NULL,
    PRIMARY KEY (id_echeance, id_paiement),
    CONSTRAINT fk_paiement_echeance_echeance FOREIGN KEY (id_echeance) REFERENCES echeance_loyer(id_echeance),
    CONSTRAINT fk_paiement_echeance_paiement FOREIGN KEY (id_paiement) REFERENCES paiement(id_paiement)
);

-- ==============================================================
-- Index
-- ==============================================================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(id_role);

CREATE INDEX idx_password_reset_token ON password_reset_token(token);
CREATE INDEX idx_password_reset_email ON password_reset_token(email);
CREATE INDEX idx_password_reset_expiration ON password_reset_token(expiration);

CREATE INDEX idx_pending_email ON pending_registration(email);
CREATE INDEX idx_pending_code ON pending_registration(code);
CREATE INDEX idx_pending_expiration ON pending_registration(expiration);

CREATE INDEX idx_medias_entite ON medias(entite_type, entite_id);
CREATE INDEX idx_medias_affichage ON medias(entite_type, entite_id, ordre);
CREATE INDEX idx_medias_principal ON medias(entite_type, entite_id) WHERE is_principal = TRUE;

CREATE INDEX idx_echeance_entite ON echeance_loyer(entite_echeance_type, entite_echeance_id);

CREATE INDEX idx_bienservice_secteur ON bien_service(id_secteur);
CREATE INDEX idx_bienservice_categorie ON bien_service(id_categorie);
CREATE INDEX idx_bienservice_user ON bien_service(id_user);
CREATE INDEX idx_cour_secteur ON cour(id_secteur);
CREATE INDEX idx_cour_user ON cour(id_user);
CREATE INDEX idx_maison_cour ON maison(id_cour);
CREATE INDEX idx_contralocation_user ON contra_location(id_user);
CREATE INDEX idx_contralocation_maison ON contra_location(id_maison);
CREATE INDEX idx_contratmandat_cour ON contrat_mandat(id_cour);
CREATE INDEX idx_contratmandat_user ON contrat_mandat(id_user);
CREATE INDEX idx_location_user ON location_bien_service(id_user);
CREATE INDEX idx_location_bienservice ON location_bien_service(id_bien_service);
CREATE INDEX idx_reservation_user ON reservation_maison(id_user);
CREATE INDEX idx_reservation_maison ON reservation_maison(id_maison);
CREATE INDEX idx_ville_pays ON ville(id_pays);
CREATE INDEX idx_secteur_ville ON secteur(id_ville);
CREATE INDEX idx_journal_user ON journal_operation(id_user);

-- ==============================================================
-- Données initiales (rôles)
-- ==============================================================
INSERT INTO role (libelle_role) VALUES 
    ('ROLE_CLIENT'),
    ('ROLE_BAILLEUR'),
    ('ROLE_AGENT'),
    ('ROLE_ADMIN')
ON CONFLICT (libelle_role) DO NOTHING;

-- ==============================================================
-- Vérification
-- ==============================================================
SELECT 'Installation terminée avec succès !' as status;
SELECT COUNT(*) as roles_crees FROM role;