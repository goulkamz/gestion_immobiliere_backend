/*==============================================================*/
/* Script de seed — données de test pour couvrir tous les modules */
/* Exécuté après 01-init.sql (schéma + rôles) au premier démarrage */
/* Mot de passe commun (clair) pour tous les comptes : Password123! */
/*==============================================================*/

-- ==============================================================
-- Users — un compte par rôle
-- ==============================================================
INSERT INTO users (id_role, nom, prenom, sexe, email, mot_de_passe, date_naissance,
                    telephone, flag_actif, date_create)
VALUES
    ((SELECT id_role FROM role WHERE libelle_role = 'ROLE_ADMIN'),
     'Admin', 'Système', 'M', 'admin@gestimmo.test',
     '$2b$10$Cd6ZzZLgeG241M75v0ub1ea7cFQNsXwIwUpj0sXEGSyoEBhrI4NMu',
     '1985-01-15', '70000001', TRUE, NOW()),

    ((SELECT id_role FROM role WHERE libelle_role = 'ROLE_AGENT'),
     'Diallo', 'Amadou', 'M', 'agent@gestimmo.test',
     '$2b$10$Cd6ZzZLgeG241M75v0ub1ea7cFQNsXwIwUpj0sXEGSyoEBhrI4NMu',
     '1990-03-22', '70000002', TRUE, NOW()),

    ((SELECT id_role FROM role WHERE libelle_role = 'ROLE_BAILLEUR'),
     'Ouedraogo', 'Fatou', 'F', 'bailleur@gestimmo.test',
     '$2b$10$Cd6ZzZLgeG241M75v0ub1ea7cFQNsXwIwUpj0sXEGSyoEBhrI4NMu',
     '1978-07-10', '70000003', TRUE, NOW()),

    ((SELECT id_role FROM role WHERE libelle_role = 'ROLE_CLIENT'),
     'Kone', 'Ibrahim', 'M', 'client@gestimmo.test',
     '$2b$10$Cd6ZzZLgeG241M75v0ub1ea7cFQNsXwIwUpj0sXEGSyoEBhrI4NMu',
     '1995-11-05', '70000004', TRUE, NOW());

-- ==============================================================
-- Localisation — pays / villes / secteurs
-- ==============================================================
INSERT INTO pays (code_pays, nom_pays) VALUES ('226', 'Burkina Faso');

INSERT INTO ville (id_pays, code_ville, nom_ville)
VALUES
    ((SELECT id_pays FROM pays WHERE code_pays = '226'), 'OUA', 'Ouagadougou'),
    ((SELECT id_pays FROM pays WHERE code_pays = '226'), 'BOB', 'Bobo-Dioulasso');

INSERT INTO secteur (id_ville, code_secteur, nom_secteur)
VALUES
    ((SELECT id_ville FROM ville WHERE code_ville = 'OUA'), 'S12', 'Secteur 12'),
    ((SELECT id_ville FROM ville WHERE code_ville = 'OUA'), 'S15', 'Secteur 15'),
    ((SELECT id_ville FROM ville WHERE code_ville = 'BOB'), 'S03', 'Secteur 3');

-- ==============================================================
-- Catégories + Biens/Services
-- ==============================================================
INSERT INTO categorie_bien_service (libelle, description, date_create)
VALUES
    ('Véhicule', 'Location de véhicules', NOW()),
    ('Équipement', 'Location de matériel événementiel', NOW());

INSERT INTO bien_service (id_secteur, id_categorie, id_user, libelle, description,
                           prix_journalier, prix_mensuel, disponibilite, date_create)
VALUES (
    (SELECT id_secteur FROM secteur WHERE code_secteur = 'S12'),
    (SELECT id_categorie FROM categorie_bien_service WHERE libelle = 'Véhicule'),
    (SELECT id_user FROM users WHERE email = 'agent@gestimmo.test'),
    'Toyota Hilux 2022', 'Pick-up 4x4 climatisé, idéal chantier ou voyage',
    35000, 800000, 'DISPONIBLE', NOW()
);

-- ==============================================================
-- Cour + Maisons
-- ==============================================================
INSERT INTO cour (id_secteur, id_user, reference_cours, lot_cours, numero_porte, date_create)
VALUES (
    (SELECT id_secteur FROM secteur WHERE code_secteur = 'S12'),
    (SELECT id_user FROM users WHERE email = 'bailleur@gestimmo.test'),
    'COUR-2026-001', 'Lot 45', 12, NOW()
);

INSERT INTO maison (id_cour, type_maison, nom_commun_maison, nombre_piece,
                     loyer, caution, nombre_mois_caution, statut, date_create)
VALUES
    ((SELECT id_cour FROM cour WHERE reference_cours = 'COUR-2026-001'),
     'Villa', 'Maison A - Villa 3 pièces', 3, 150000, 300000, 2, 'LOUEE', NOW()),

    ((SELECT id_cour FROM cour WHERE reference_cours = 'COUR-2026-001'),
     'Studio', 'Maison B - Studio', 1, 60000, 120000, 2, 'DISPONIBLE', NOW());

-- ==============================================================
-- Contrat de mandat (bailleur <-> agent, via la cour)
-- ==============================================================
INSERT INTO contrat_mandat (id_cour, id_user, date_debut, date_fin, type_mandat,
                             commission, mode_facturation, statut)
VALUES (
    (SELECT id_cour FROM cour WHERE reference_cours = 'COUR-2026-001'),
    (SELECT id_user FROM users WHERE email = 'agent@gestimmo.test'),
    NOW(), NOW() + INTERVAL '1 year', 'GESTION',
    10, 'MENSUEL', 'ACTIF'
);

-- ==============================================================
-- Contrat de location (client <-> Maison A, déjà LOUEE)
-- ==============================================================
INSERT INTO contra_location (id_user, id_maison, date_entree, date_sortie,
                              montant_loyer, statut, type_contrat, depot_garantie,
                              etat_des_lieux_entree, date_create)
VALUES (
    (SELECT id_user FROM users WHERE email = 'client@gestimmo.test'),
    (SELECT id_maison FROM maison WHERE nom_commun_maison = 'Maison A - Villa 3 pièces'),
    NOW(), NOW() + INTERVAL '11 months',
    150000, 'ACTIF', 'HABITATION', 300000,
    'RAS - état correct à l''entrée', NOW()
);

-- ==============================================================
-- Échéances de loyer (2 mois, une payée, une en attente)
-- ==============================================================
INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance,
                             montant_du, montant_paye, statut, date_create)
VALUES
    ('LOCATION',
     (SELECT id_contra_location FROM contra_location LIMIT 1),
     CURRENT_DATE - INTERVAL '1 month', 150000, 150000, 'PAYE', NOW()),

    ('LOCATION',
     (SELECT id_contra_location FROM contra_location LIMIT 1),
     CURRENT_DATE, 150000, 0, 'EN_ATTENTE', NOW());

-- ==============================================================
-- Paiement (couvre la 1ère échéance)
-- ==============================================================
INSERT INTO paiement (date_paiement, montant_paiement, mode_paiement, reference_paiement, user_create)
VALUES (
    NOW() - INTERVAL '1 month', 150000, 'MOBILE_MONEY', 'PAY-2026-0001',
    (SELECT id_user FROM users WHERE email = 'client@gestimmo.test')
);

INSERT INTO paiement_echeance (id_echeance, id_paiement)
VALUES (
    (SELECT id_echeance FROM echeance_loyer WHERE statut = 'PAYE' LIMIT 1),
    (SELECT id_paiement FROM paiement LIMIT 1)
);

-- ==============================================================
-- Annonces
-- ==============================================================
INSERT INTO annonce (titre, description, type_annonce, date_publication, date_expiration,
                      statut, prix, localisation)
VALUES
    ('Studio à louer - Secteur 12', 'Studio meublé, calme, proche marché',
     'LOCATION', NOW(), NOW() + INTERVAL '30 days', 'ACTIVE', 60000, 'Ouagadougou, Secteur 12'),

    ('Villa 3 pièces disponible bientôt', 'Villa moderne, cour clôturée',
     'LOCATION', NOW(), NOW() + INTERVAL '15 days', 'ACTIVE', 150000, 'Ouagadougou, Secteur 12');

-- ==============================================================
-- Demande (F18)
-- ==============================================================
INSERT INTO demande (nom_complet, email, telephone, type_bien, localisation_souhaite,
                      budget_max, description, date_demande, statut)
VALUES (
    'Sana Awa', 'sana.awa@test.com', '70123456', 'Appartement',
    'Ouagadougou', 100000, 'Recherche 2 pièces proche université',
    NOW(), 'EN_ATTENTE'
);

-- ==============================================================
-- Offre (F19)
-- ==============================================================
INSERT INTO offre (nom_complet, email, telephone, type_offre, titre, description,
                    adresse, date_offre, statut)
VALUES (
    'Traore Boureima', 'traore.b@test.com', '70654321', 'MAISON',
    'Cour à confier en gestion', 'Cour de 4 maisons, secteur 15, bon état',
    'Secteur 15, Ouagadougou', NOW(), 'ACTIVE'
);

-- ==============================================================
-- Contact (F20)
-- ==============================================================
INSERT INTO contact (nom_complet, email, telephone, sujet, message, date_envoi, statut)
VALUES (
    'Zongo Salif', 'zongo.salif@test.com', '70999888', 'Question sur une annonce',
    'Bonjour, le studio du secteur 12 est-il toujours disponible ?', NOW(), 'NON_LU'
);

-- ==============================================================
-- Réservation (F21 — sur la Maison B, disponible)
-- ==============================================================
INSERT INTO reservation_maison (id_user, id_maison, date_debut, date_fin, statut)
VALUES (
    (SELECT id_user FROM users WHERE email = 'client@gestimmo.test'),
    (SELECT id_maison FROM maison WHERE nom_commun_maison = 'Maison B - Studio'),
    NOW() + INTERVAL '5 days', NOW() + INTERVAL '10 days', 'EN_ATTENTE'
);

-- ==============================================================
-- Vérification finale
-- ==============================================================
SELECT 'Seed terminé avec succès !' AS status;
SELECT
    (SELECT COUNT(*) FROM users) AS nb_users,
    (SELECT COUNT(*) FROM cour) AS nb_cours,
    (SELECT COUNT(*) FROM maison) AS nb_maisons,
    (SELECT COUNT(*) FROM contrat_mandat) AS nb_mandats,
    (SELECT COUNT(*) FROM contra_location) AS nb_locations,
    (SELECT COUNT(*) FROM annonce) AS nb_annonces;