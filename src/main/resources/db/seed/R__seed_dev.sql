/*==============================================================*/
/* Seed de DEVELOPPEMENT : donnees de base uniquement.           */
/* 1 admin, 3 agents, 5 bailleurs, 10 clients, 3 villes,         */
/* 10 cours, 50 maisons, 5 categories, 30 biens/services,        */
/* 25 annonces, 15 contacts, 60 offres, 40 demandes.             */
/* Mandats, contrats, echeances, paiements, reservations et      */
/* locations de biens/services ne sont PAS semes : ils se creent */
/* en manipulant l'application (toutes les maisons sont libres). */
/* Mot de passe commun (clair) pour tous les comptes : Password123! */
/* Les parametres systeme viennent de la migration V3.           */
/*==============================================================*/

TRUNCATE TABLE remboursement, paiement_location_bien_service, location_bien_service,
    paiement_echeance, echeance_loyer, paiement, contra_location, contrat_mandat,
    maison, cour, bien_service, categorie_bien_service, reservation_maison,
    contact, offre, temoignage, demande, annonce, secteur, ville, pays, users
RESTART IDENTITY CASCADE;

-- ==============================================================
-- Users : emails du type agent@, agent2@, agent3@, bailleur@, bailleur2@ ... client@, client2@ ...
-- ==============================================================
INSERT INTO users (id_role, nom, prenom, sexe, email, mot_de_passe, date_naissance, telephone, flag_actif, created_at)
SELECT (SELECT id_role FROM role WHERE libelle_role = u.role), u.nom, u.prenom, u.sexe, u.email,
       '$2b$10$Cd6ZzZLgeG241M75v0ub1ea7cFQNsXwIwUpj0sXEGSyoEBhrI4NMu',
       DATE '1975-01-01' + (u.n * 397), '7000' || LPAD(u.n::text, 4, '0'), TRUE, NOW() - (u.n * INTERVAL '3 days')
FROM (VALUES
    (1,  'ROLE_ADMIN',    'Zongo',     'Mariam',   'F', 'admin@gestimmo.test'),
    (2,  'ROLE_AGENT',    'Diallo',    'Amadou',   'M', 'agent@gestimmo.test'),
    (3,  'ROLE_AGENT',    'Sankara',   'Aminata',  'F', 'agent2@gestimmo.test'),
    (4,  'ROLE_AGENT',    'Tapsoba',   'Issouf',   'M', 'agent3@gestimmo.test'),
    (5,  'ROLE_BAILLEUR', 'Ouedraogo', 'Fatou',    'F', 'bailleur@gestimmo.test'),
    (6,  'ROLE_BAILLEUR', 'Compaore',  'Jean',     'M', 'bailleur2@gestimmo.test'),
    (7,  'ROLE_BAILLEUR', 'Nikiema',   'Salamata', 'F', 'bailleur3@gestimmo.test'),
    (8,  'ROLE_BAILLEUR', 'Barry',     'Moussa',   'M', 'bailleur4@gestimmo.test'),
    (9,  'ROLE_BAILLEUR', 'Ilboudo',   'Rasmata',  'F', 'bailleur5@gestimmo.test'),
    (10, 'ROLE_CLIENT',   'Kone',      'Ibrahim',  'M', 'client@gestimmo.test'),
    (11, 'ROLE_CLIENT',   'Sawadogo',  'Fatimata', 'F', 'client2@gestimmo.test'),
    (12, 'ROLE_CLIENT',   'Kabore',    'Seydou',   'M', 'client3@gestimmo.test'),
    (13, 'ROLE_CLIENT',   'Ouattara',  'Awa',      'F', 'client4@gestimmo.test'),
    (14, 'ROLE_CLIENT',   'Zida',      'Boukary',  'M', 'client5@gestimmo.test'),
    (15, 'ROLE_CLIENT',   'Yameogo',   'Clarisse', 'F', 'client6@gestimmo.test'),
    (16, 'ROLE_CLIENT',   'Some',      'Lassina',  'M', 'client7@gestimmo.test'),
    (17, 'ROLE_CLIENT',   'Traore',    'Nadège',   'F', 'client8@gestimmo.test'),
    (18, 'ROLE_CLIENT',   'Belem',     'Hamidou',  'M', 'client9@gestimmo.test'),
    (19, 'ROLE_CLIENT',   'Coulibaly', 'Estelle',  'F', 'client10@gestimmo.test')
) AS u(n, role, nom, prenom, sexe, email);

-- ==============================================================
-- Localisation : 1 pays, 3 villes, 9 secteurs
-- ==============================================================
INSERT INTO pays (code_pays, nom_pays) VALUES ('226', 'Burkina Faso');

INSERT INTO ville (id_pays, code_ville, nom_ville)
SELECT (SELECT id_pays FROM pays WHERE code_pays = '226'), v.code, v.nom
FROM (VALUES ('OUA', 'Ouagadougou'), ('BOB', 'Bobo-Dioulasso'), ('KDG', 'Koudougou')) AS v(code, nom);

INSERT INTO secteur (id_ville, code_secteur, nom_secteur)
SELECT (SELECT id_ville FROM ville WHERE code_ville = s.ville), s.code, s.nom
FROM (VALUES
    ('OUA', 'S12', 'Secteur 12'),
    ('OUA', 'S15', 'Secteur 15'),
    ('OUA', 'S30', 'Secteur 30'),
    ('OUA', 'S05', 'Secteur 5'),
    ('BOB', 'S03', 'Secteur 3'),
    ('BOB', 'S07', 'Secteur 7'),
    ('BOB', 'S22', 'Secteur 22'),
    ('KDG', 'K01', 'Secteur 1'),
    ('KDG', 'K04', 'Secteur 4')
) AS s(ville, code, nom);

-- ==============================================================
-- 10 cours : chaque bailleur en possede 2, repartis sur les 3 villes
-- ==============================================================
INSERT INTO cour (id_secteur, id_user, reference_cour, lot_cour, numero_porte, description, created_at)
SELECT (SELECT id_secteur FROM secteur ORDER BY id_secteur OFFSET ((n * 4) % 9) LIMIT 1),
       (SELECT id_user FROM users WHERE email = CASE (n + 1) / 2 WHEN 1 THEN 'bailleur@gestimmo.test'
            ELSE 'bailleur' || ((n + 1) / 2) || '@gestimmo.test' END),
       'COUR-2026-' || LPAD(n::text, 3, '0'),
       'Lot ' || (10 + n * 7),
       n * 3,
       'Cour clôturée avec 5 logements, point d''eau et électricité, gardien de nuit.',
       NOW() - (n * INTERVAL '10 days')
FROM generate_series(1, 10) AS n;

-- ==============================================================
-- 50 maisons : 5 par cour, toutes DISPONIBLE (aucun mandat ni contrat)
-- ==============================================================
INSERT INTO maison (id_cour, type_maison, nom_commun_maison, nombre_piece, loyer, caution, nombre_mois_caution, statut, description, created_at)
SELECT c.id_cour, t.type, t.type || ' ' || c.id_cour || '-' || g.num, t.pieces, t.loyer,
       t.loyer * 2, 2, 'DISPONIBLE', t.description, NOW() - (c.id_cour * INTERVAL '9 days')
FROM cour c
CROSS JOIN generate_series(1, 5) AS g(num)
JOIN (VALUES
    (1, 'Studio',          1,  45000, 'Studio avec douche interne, carrelé, proche des commerces.'),
    (2, 'Chambre-salon',   2,  65000, 'Une chambre et un salon, cuisine externe, douche interne.'),
    (3, 'Appartement',     3,  95000, 'Appartement de 3 pièces, cuisine équipée, balcon.'),
    (4, 'Villa',           4, 150000, 'Villa de 4 pièces avec cour privée et place de parking.'),
    (5, 'Duplex',          5, 220000, 'Duplex de 5 pièces avec terrasse, climatisation prévue.')
) AS t(k, type, pieces, loyer, description) ON t.k = ((g.num + c.id_cour) % 5) + 1;

-- ==============================================================
-- 5 categories + 30 biens/services (6 par categorie), proposes par les 3 agents
-- ==============================================================
INSERT INTO categorie_bien_service (libelle, description, created_at)
VALUES
    ('Véhicule', 'Location de véhicules avec ou sans chauffeur', NOW()),
    ('Équipement événementiel', 'Sonorisation, bâches, chaises et tables', NOW()),
    ('Matériel de chantier', 'Outillage et engins pour travaux', NOW()),
    ('Salle et espace', 'Salles de réunion, de fête et espaces de stockage', NOW()),
    ('Électroménager', 'Appareils à louer pour la maison ou le bureau', NOW());

INSERT INTO bien_service (id_secteur, id_categorie, id_user, libelle, description, prix_journalier, prix_mensuel, disponibilite, created_at)
SELECT (SELECT id_secteur FROM secteur ORDER BY id_secteur OFFSET (b.n % 9) LIMIT 1),
       (SELECT id_categorie FROM categorie_bien_service WHERE libelle = b.categorie),
       (SELECT id_user FROM users WHERE email = CASE b.n % 3 WHEN 0 THEN 'agent@gestimmo.test'
            WHEN 1 THEN 'agent2@gestimmo.test' ELSE 'agent3@gestimmo.test' END),
       b.libelle, b.description, b.prix_jour, b.prix_mois, 'DISPONIBLE', NOW() - (b.n * INTERVAL '2 days')
FROM (VALUES
    (1,  'Véhicule', 'Toyota Hilux 2022', 'Pick-up 4x4 climatisé, idéal chantier ou voyage', 35000, 800000),
    (2,  'Véhicule', 'Toyota Corolla 2020', 'Berline économique et confortable', 25000, 550000),
    (3,  'Véhicule', 'Minibus Toyota Hiace 18 places', 'Transport de groupe, climatisé', 60000, 1300000),
    (4,  'Véhicule', 'Hyundai Tucson 2021', 'SUV familial, boîte automatique', 40000, 900000),
    (5,  'Véhicule', 'Moto Yamaha 125', 'Moto de ville, casque fourni', 6000, 120000),
    (6,  'Véhicule', 'Camion benne 10 tonnes', 'Transport de matériaux, chauffeur disponible', 90000, NULL),
    (7,  'Équipement événementiel', 'Kit sonorisation 500W', 'Sono complète avec micros et enceintes', 15000, NULL),
    (8,  'Équipement événementiel', 'Bâche 100 places', 'Grande bâche avec montage et démontage', 50000, NULL),
    (9,  'Équipement événementiel', 'Lot de 100 chaises', 'Chaises plastiques empilables', 20000, NULL),
    (10, 'Équipement événementiel', 'Groupe électrogène 20 kVA', 'Alimentation de secours pour événement', 30000, 600000),
    (11, 'Équipement événementiel', 'Vidéoprojecteur et écran', 'Projecteur HD avec écran de 3 mètres', 12000, NULL),
    (12, 'Équipement événementiel', 'Lot de 20 tables rondes', 'Tables de banquet pour 8 personnes', 25000, NULL),
    (13, 'Matériel de chantier', 'Bétonnière 350 L', 'Bétonnière électrique sur roues', 10000, 200000),
    (14, 'Matériel de chantier', 'Échafaudage 50 m²', 'Éléments modulaires avec stabilisateurs', 18000, 350000),
    (15, 'Matériel de chantier', 'Perforateur burineur', 'Perforateur professionnel avec jeu de forets', 7000, NULL),
    (16, 'Matériel de chantier', 'Compacteur à plaque', 'Compactage de tranchées et de plateformes', 20000, NULL),
    (17, 'Matériel de chantier', 'Mini-pelle 1,8 tonne', 'Terrassement avec conducteur', 120000, NULL),
    (18, 'Matériel de chantier', 'Poste à souder inverter', 'Poste 200 A avec électrodes', 8000, NULL),
    (19, 'Salle et espace', 'Salle de réunion 20 places', 'Salle climatisée avec paperboard et wifi', 30000, 600000),
    (20, 'Salle et espace', 'Salle des fêtes 200 places', 'Salle avec scène, cuisine et parking', 150000, NULL),
    (21, 'Salle et espace', 'Entrepôt de 150 m²', 'Stockage sécurisé avec quai de chargement', 20000, 450000),
    (22, 'Salle et espace', 'Bureau partagé', 'Poste de travail avec connexion fibre', 5000, 90000),
    (23, 'Salle et espace', 'Terrain de réception', 'Espace extérieur clôturé de 1000 m²', 70000, NULL),
    (24, 'Salle et espace', 'Salle de formation 30 places', 'Salle équipée de vidéoprojecteur', 45000, 900000),
    (25, 'Électroménager', 'Réfrigérateur 300 L', 'Réfrigérateur double porte', 3000, 60000),
    (26, 'Électroménager', 'Climatiseur split 2 CV', 'Climatiseur avec installation incluse', 5000, 100000),
    (27, 'Électroménager', 'Machine à laver 8 kg', 'Lave-linge chargement frontal', 3500, 70000),
    (28, 'Électroménager', 'Congélateur coffre 400 L', 'Idéal pour événements et commerces', 4000, 80000),
    (29, 'Électroménager', 'Cuisinière 4 feux avec four', 'Cuisinière à gaz', 3000, 55000),
    (30, 'Électroménager', 'Lot de 3 ventilateurs sur pied', 'Ventilateurs puissants pour salle ou bureau', 2500, 45000)
) AS b(n, categorie, libelle, description, prix_jour, prix_mois);

-- ==============================================================
-- 25 annonces : 20 ACTIVE, 3 EXPIREE, 2 SUSPENDUE ; 20 LOCATION, 5 VENTE
-- ==============================================================
INSERT INTO annonce (titre, description, type_annonce, date_publication, date_expiration, statut, prix, localisation)
SELECT CASE WHEN n % 5 = 0 THEN 'Terrain à vendre - ' ELSE t.type || ' à louer - ' END || s.nom_secteur,
       CASE WHEN n % 5 = 0 THEN 'Parcelle viabilisée, titre foncier disponible, accès facile.'
            ELSE t.type || ' en bon état, ' || t.pieces || ' pièce(s), eau et électricité, quartier calme.' END,
       CASE WHEN n % 5 = 0 THEN 'VENTE' ELSE 'LOCATION' END,
       NOW() - (n * INTERVAL '2 days'),
       CASE WHEN n IN (3, 11, 19) THEN NOW() - INTERVAL '1 day' ELSE NOW() + INTERVAL '30 days' END,
       CASE WHEN n IN (3, 11, 19) THEN 'EXPIREE' WHEN n IN (8, 22) THEN 'SUSPENDUE' ELSE 'ACTIVE' END,
       CASE WHEN n % 5 = 0 THEN 8000000 + n * 250000 ELSE t.loyer END,
       v.nom_ville || ', ' || s.nom_secteur
FROM generate_series(1, 25) AS n
JOIN (SELECT ROW_NUMBER() OVER (ORDER BY id_secteur) AS r, id_secteur, id_ville, nom_secteur FROM secteur) s ON s.r = ((n - 1) % 9) + 1
JOIN ville v ON v.id_ville = s.id_ville
JOIN (VALUES
    (0, 'Studio', 1, 45000), (1, 'Chambre-salon', 2, 65000), (2, 'Appartement', 3, 95000),
    (3, 'Villa', 4, 150000), (4, 'Duplex', 5, 220000)
) AS t(k, type, pieces, loyer) ON t.k = n % 5;

-- ==============================================================
-- 15 contacts : 6 NON_LU, 5 LU, 4 TRAITE
-- ==============================================================
INSERT INTO contact (nom_complet, email, telephone, sujet, message, date_envoi, statut)
SELECT p.prenom || ' ' || p.nom,
       LOWER(p.prenom) || '.' || LOWER(p.nom) || '@test.com',
       '7600' || LPAD(n::text, 4, '0'),
       sj.sujet, sj.message, NOW() - (n * INTERVAL '1 day' + n * INTERVAL '3 hours'),
       CASE WHEN n <= 6 THEN 'NON_LU' WHEN n <= 11 THEN 'LU' ELSE 'TRAITE' END
FROM generate_series(1, 15) AS n
JOIN (VALUES
    (0, 'Zongo', 'Salif'), (1, 'Kinda', 'Mariam'), (2, 'Ouedraogo', 'Adama'), (3, 'Sanou', 'Rokia'),
    (4, 'Bationo', 'Elie'), (5, 'Dabire', 'Pascaline'), (6, 'Konate', 'Drissa'), (7, 'Lankoande', 'Estelle')
) AS p(k, nom, prenom) ON p.k = n % 8
JOIN (VALUES
    (0, 'Disponibilité d''une annonce', 'Bonjour, l''annonce que j''ai vue est-elle toujours disponible ?'),
    (1, 'Demande de visite', 'Je souhaite visiter une maison cette semaine, quels sont vos horaires ?'),
    (2, 'Confier ma cour en gestion', 'Je possède une cour et je voudrais connaître vos conditions de gestion.'),
    (3, 'Location de véhicule', 'Quels sont les tarifs pour un pick-up sur une semaine ?'),
    (4, 'Problème avec mon compte', 'Je n''arrive plus à me connecter à mon espace, pouvez-vous m''aider ?')
) AS sj(k, sujet, message) ON sj.k = n % 5;

-- ==============================================================
-- 60 offres : 40 EN_ATTENTE, 12 TRAITEE, 8 REFUSEE
-- ==============================================================
INSERT INTO offre (nom_complet, email, telephone, type_offre, titre, description, adresse, date_offre, statut)
SELECT p.prenom || ' ' || p.nom,
       LOWER(p.prenom) || '.' || LOWER(p.nom) || n || '@test.com',
       '7500' || LPAD(n::text, 4, '0'),
       t.type, t.titre, t.description,
       s.nom_secteur || ', ' || v.nom_ville,
       NOW() - (n * INTERVAL '14 hours'),
       CASE WHEN n % 5 = 0 THEN 'TRAITEE' WHEN n % 7 = 0 THEN 'REFUSEE' ELSE 'EN_ATTENTE' END
FROM generate_series(1, 60) AS n
JOIN (VALUES
    (0, 'Sawadogo', 'Boureima'), (1, 'Traore', 'Aïcha'), (2, 'Ouedraogo', 'Paul'), (3, 'Kabore', 'Rasmata'),
    (4, 'Compaore', 'Hamidou'), (5, 'Nikiema', 'Judith'), (6, 'Zerbo', 'Tiéba'), (7, 'Sory', 'Mariétou'),
    (8, 'Diarra', 'Oumar'), (9, 'Bambara', 'Clarisse')
) AS p(k, nom, prenom) ON p.k = n % 10
JOIN (VALUES
    (0, 'MAISON',   'Cour à confier en gestion', 'Cour de plusieurs logements en bon état, à confier à l''agence.'),
    (1, 'MAISON',   'Villa à louer', 'Villa de 4 pièces avec cour privée, libre immédiatement.'),
    (2, 'TERRAIN',  'Parcelle à vendre', 'Parcelle de 600 m² avec attestation d''attribution.'),
    (3, 'MAISON',   'Studios à mettre en location', 'Quatre studios neufs, je cherche une agence pour la gestion.'),
    (4, 'VEHICULE', 'Véhicule à mettre en location', 'Pick-up en excellent état, disponible pour la location.'),
    (5, 'AUTRE',    'Salle des fêtes à proposer', 'Salle de 150 places avec parking, à proposer à votre clientèle.')
) AS t(k, type, titre, description) ON t.k = n % 6
JOIN (SELECT ROW_NUMBER() OVER (ORDER BY id_secteur) AS r, id_ville, nom_secteur FROM secteur) s ON s.r = ((n - 1) % 9) + 1
JOIN ville v ON v.id_ville = s.id_ville;

-- ==============================================================
-- 40 demandes : 20 EN_ATTENTE, 10 EN_COURS, 6 SATISFAITE, 4 ANNULEE
-- ==============================================================
INSERT INTO demande (nom_complet, email, telephone, type_bien, localisation_souhaite, budget_max, description, date_demande, statut)
SELECT p.prenom || ' ' || p.nom,
       LOWER(p.prenom) || '.' || LOWER(p.nom) || n || '@test.com',
       '7400' || LPAD(n::text, 4, '0'),
       t.type, v.nom_ville, t.budget,
       t.description,
       NOW() - (n * INTERVAL '18 hours'),
       CASE WHEN n % 10 IN (1, 2) THEN 'EN_COURS' WHEN n % 20 = 3 THEN 'SATISFAITE' WHEN n % 10 = 4 THEN 'ANNULEE'
            WHEN n % 20 = 13 THEN 'SATISFAITE' WHEN n % 20 = 18 THEN 'SATISFAITE' WHEN n % 20 = 8 THEN 'SATISFAITE'
            ELSE 'EN_ATTENTE' END
FROM generate_series(1, 40) AS n
JOIN (VALUES
    (0, 'Sana', 'Awa'), (1, 'Kiemde', 'Boukary'), (2, 'Zoungrana', 'Alice'), (3, 'Ouoba', 'Daouda'),
    (4, 'Tiendrebeogo', 'Lydie'), (5, 'Kafando', 'Moussa'), (6, 'Thiombiano', 'Sylvie'), (7, 'Nana', 'Issa'),
    (8, 'Bagagnan', 'Fanta'), (9, 'Pare', 'Wendyam')
) AS p(k, nom, prenom) ON p.k = n % 10
JOIN (VALUES
    (0, 'Studio',        50000, 'Recherche un studio proche du centre-ville, eau et électricité.'),
    (1, 'Chambre-salon', 70000, 'Recherche une chambre-salon calme pour un couple.'),
    (2, 'Appartement',  100000, 'Recherche un appartement de 3 pièces proche d''une école.'),
    (3, 'Villa',        160000, 'Recherche une villa avec cour pour une famille.'),
    (4, 'Duplex',       230000, 'Recherche un duplex pour un cadre expatrié.')
) AS t(k, type, budget, description) ON t.k = n % 5
JOIN (SELECT ROW_NUMBER() OVER (ORDER BY id_ville) AS r, nom_ville FROM ville) v ON v.r = ((n * 7) % 3) + 1;

SELECT 'Seed terminé avec succès !' AS status;
SELECT
    (SELECT COUNT(*) FROM users) AS nb_users,
    (SELECT COUNT(*) FROM cour) AS nb_cours,
    (SELECT COUNT(*) FROM maison) AS nb_maisons,
    (SELECT COUNT(*) FROM categorie_bien_service) AS nb_categories,
    (SELECT COUNT(*) FROM bien_service) AS nb_biens_services,
    (SELECT COUNT(*) FROM annonce) AS nb_annonces,
    (SELECT COUNT(*) FROM contact) AS nb_contacts,
    (SELECT COUNT(*) FROM offre) AS nb_offres,
    (SELECT COUNT(*) FROM demande) AS nb_demandes;
