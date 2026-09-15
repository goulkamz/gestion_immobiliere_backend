/*==============================================================*/
/* Script de seed COMPLET — couvre tous les modules + les corrections */
/* de cette session : échéances calendaires, reversement bailleur,   */
/* remboursement lié à paiement, résiliation avec nettoyage.         */
/* Idempotent dans le sens où il utilise des dates relatives à NOW().*/
/* Mot de passe commun (clair) pour tous les comptes : Password123!  */
/*==============================================================*/

TRUNCATE TABLE remboursement, paiement_location_bien_service, location_bien_service,
    paiement_echeance, echeance_loyer, paiement, contra_location, contrat_mandat,
    maison, cour, bien_service, categorie_bien_service, reservation_maison,
    contact, offre, demande, annonce, secteur, ville, pays, users
RESTART IDENTITY CASCADE;


DO $$
DECLARE
    mois_courant        DATE := DATE_TRUNC('month', CURRENT_DATE)::date;
    mois_precedent       DATE := (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '1 month')::date;
    mois_avant_precedent DATE := (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '2 months')::date;
    mois_suivant         DATE := (DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month')::date;

    id_admin      INTEGER;
    id_agent       INTEGER;
    id_bailleur1   INTEGER;
    id_bailleur2   INTEGER;
    id_client1     INTEGER;
    id_client2     INTEGER;

    id_secteur_s12 INTEGER;
    id_secteur_s15 INTEGER;
    id_secteur_s03 INTEGER;

    id_cour1 INTEGER; -- Ouagadougou, bailleur1
    id_cour2 INTEGER; -- Bobo-Dioulasso, bailleur2

    id_maison_a INTEGER; -- LOUEE (client1) - cour1
    id_maison_b INTEGER; -- DISPONIBLE - cour1
    id_maison_e INTEGER; -- LOUEE puis RESILIEE mi-mois - cour1
    id_maison_c INTEGER; -- LOUEE (client2) - cour2
    id_maison_d INTEGER; -- DISPONIBLE - cour2

    id_mandat1 INTEGER; -- cour1, commission 10%
    id_mandat2 INTEGER; -- cour2, commission 12%

    id_contrat1 INTEGER; -- client1 / maison A, ACTIF
    id_contrat2 INTEGER; -- client2 / maison C, ACTIF
    id_contrat3 INTEGER; -- client1 / maison E, RESILIE mi-mois

    id_categorie_vehicule INTEGER;
    id_categorie_equipement INTEGER;
    id_bien_vehicule INTEGER;
    id_bien_sono INTEGER;

    id_paiement_loyer_a_precedent INTEGER;
    id_paiement_loyer_c_precedent INTEGER;
    id_paiement_reversement_mandat1_ancien INTEGER;
    id_paiement_remboursement INTEGER;

    id_echeance_a_retard INTEGER;
    id_echeance_a_payee INTEGER;
    id_echeance_c_payee INTEGER;
    id_echeance_mandat1_ancienne INTEGER;
BEGIN

-- ==============================================================
-- Users
-- ==============================================================
INSERT INTO users (id_role, nom, prenom, sexe, email, mot_de_passe, date_naissance, telephone, flag_actif, created_at)
VALUES
    ((SELECT id_role FROM role WHERE libelle_role = 'ROLE_ADMIN'), 'Admin', 'Système', 'M', 'admin@gestimmo.test',
     '$2b$10$Cd6ZzZLgeG241M75v0ub1ea7cFQNsXwIwUpj0sXEGSyoEBhrI4NMu', '1985-01-15', '70000001', TRUE, NOW()),
    ((SELECT id_role FROM role WHERE libelle_role = 'ROLE_AGENT'), 'Diallo', 'Amadou', 'M', 'agent@gestimmo.test',
     '$2b$10$Cd6ZzZLgeG241M75v0ub1ea7cFQNsXwIwUpj0sXEGSyoEBhrI4NMu', '1990-03-22', '70000002', TRUE, NOW()),
    ((SELECT id_role FROM role WHERE libelle_role = 'ROLE_BAILLEUR'), 'Ouedraogo', 'Fatou', 'F', 'bailleur@gestimmo.test',
     '$2b$10$Cd6ZzZLgeG241M75v0ub1ea7cFQNsXwIwUpj0sXEGSyoEBhrI4NMu', '1978-07-10', '70000003', TRUE, NOW()),
    ((SELECT id_role FROM role WHERE libelle_role = 'ROLE_BAILLEUR'), 'Compaore', 'Jean', 'M', 'bailleur2@gestimmo.test',
     '$2b$10$Cd6ZzZLgeG241M75v0ub1ea7cFQNsXwIwUpj0sXEGSyoEBhrI4NMu', '1982-09-02', '70000006', TRUE, NOW()),
    ((SELECT id_role FROM role WHERE libelle_role = 'ROLE_CLIENT'), 'Kone', 'Ibrahim', 'M', 'client@gestimmo.test',
     '$2b$10$Cd6ZzZLgeG241M75v0ub1ea7cFQNsXwIwUpj0sXEGSyoEBhrI4NMu', '1995-11-05', '70000004', TRUE, NOW()),
    ((SELECT id_role FROM role WHERE libelle_role = 'ROLE_CLIENT'), 'Sawadogo', 'Fatimata', 'F', 'client2@gestimmo.test',
     '$2b$10$Cd6ZzZLgeG241M75v0ub1ea7cFQNsXwIwUpj0sXEGSyoEBhrI4NMu', '1993-06-18', '70000005', TRUE, NOW());

SELECT id_user INTO id_admin FROM users WHERE email = 'admin@gestimmo.test';
SELECT id_user INTO id_agent FROM users WHERE email = 'agent@gestimmo.test';
SELECT id_user INTO id_bailleur1 FROM users WHERE email = 'bailleur@gestimmo.test';
SELECT id_user INTO id_bailleur2 FROM users WHERE email = 'bailleur2@gestimmo.test';
SELECT id_user INTO id_client1 FROM users WHERE email = 'client@gestimmo.test';
SELECT id_user INTO id_client2 FROM users WHERE email = 'client2@gestimmo.test';

-- ==============================================================
-- Localisation
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

SELECT id_secteur INTO id_secteur_s12 FROM secteur WHERE code_secteur = 'S12';
SELECT id_secteur INTO id_secteur_s15 FROM secteur WHERE code_secteur = 'S15';
SELECT id_secteur INTO id_secteur_s03 FROM secteur WHERE code_secteur = 'S03';

-- ==============================================================
-- Catégories + Biens/Services
-- ==============================================================
INSERT INTO categorie_bien_service (libelle, description, created_at)
VALUES
    ('Véhicule', 'Location de véhicules', NOW()),
    ('Équipement', 'Location de matériel événementiel', NOW());

SELECT id_categorie INTO id_categorie_vehicule FROM categorie_bien_service WHERE libelle = 'Véhicule';
SELECT id_categorie INTO id_categorie_equipement FROM categorie_bien_service WHERE libelle = 'Équipement';

INSERT INTO bien_service (id_secteur, id_categorie, id_user, libelle, description, prix_journalier, prix_mensuel, disponibilite, created_at)
VALUES
    (id_secteur_s12, id_categorie_vehicule, id_agent, 'Toyota Hilux 2022', 'Pick-up 4x4 climatisé, idéal chantier ou voyage', 35000, 800000, 'DISPONIBLE', NOW()),
    (id_secteur_s15, id_categorie_equipement, id_agent, 'Kit sonorisation 500W', 'Sono complète avec micros et enceintes', 15000, NULL, 'DISPONIBLE', NOW());

SELECT id_bien_service INTO id_bien_vehicule FROM bien_service WHERE libelle = 'Toyota Hilux 2022';
SELECT id_bien_service INTO id_bien_sono FROM bien_service WHERE libelle = 'Kit sonorisation 500W';

-- ==============================================================
-- Cours (2 bailleurs, 2 villes)
-- ==============================================================
INSERT INTO cour (id_secteur, id_user, reference_cour, lot_cour, numero_porte, created_at)
VALUES
    (id_secteur_s12, id_bailleur1, 'COUR-2026-001', 'Lot 45', 12, NOW()),
    (id_secteur_s03, id_bailleur2, 'COUR-2026-002', 'Lot 8', 3, NOW());

SELECT id_cour INTO id_cour1 FROM cour WHERE reference_cour = 'COUR-2026-001';
SELECT id_cour INTO id_cour2 FROM cour WHERE reference_cour = 'COUR-2026-002';

-- ==============================================================
-- Maisons — cour1 : A (louée), B (disponible), E (louée puis résiliée)
--            cour2 : C (louée), D (disponible)
-- ==============================================================
INSERT INTO maison (id_cour, type_maison, nom_commun_maison, nombre_piece, loyer, caution, nombre_mois_caution, statut, created_at)
VALUES
    (id_cour1, 'Villa', 'Maison A - Villa 3 pièces', 3, 150000, 300000, 2, 'LOUEE', NOW()),
    (id_cour1, 'Studio', 'Maison B - Studio', 1, 60000, 120000, 2, 'DISPONIBLE', NOW()),
    (id_cour1, 'Studio', 'Maison E - Studio annexe', 1, 70000, 140000, 2, 'DISPONIBLE', NOW()), -- redevenue disponible après résiliation
    (id_cour2, 'Villa', 'Maison C - Villa 2 pièces', 2, 100000, 200000, 2, 'LOUEE', NOW()),
    (id_cour2, 'Studio', 'Maison D - Studio', 1, 55000, 110000, 2, 'DISPONIBLE', NOW());

SELECT id_maison INTO id_maison_a FROM maison WHERE nom_commun_maison = 'Maison A - Villa 3 pièces';
SELECT id_maison INTO id_maison_b FROM maison WHERE nom_commun_maison = 'Maison B - Studio';
SELECT id_maison INTO id_maison_e FROM maison WHERE nom_commun_maison = 'Maison E - Studio annexe';
SELECT id_maison INTO id_maison_c FROM maison WHERE nom_commun_maison = 'Maison C - Villa 2 pièces';
SELECT id_maison INTO id_maison_d FROM maison WHERE nom_commun_maison = 'Maison D - Studio';

-- ==============================================================
-- Contrats de mandat — 2 mandats ACTIF, même agent, commissions différentes
-- ==============================================================
INSERT INTO contrat_mandat (id_cour, id_user, date_debut, date_fin, type_mandat, commission, mode_facturation, statut)
VALUES
    (id_cour1, id_agent, mois_avant_precedent - INTERVAL '2 months', mois_avant_precedent - INTERVAL '2 months' + INTERVAL '1 year', 'GESTION', 10, 'MENSUEL', 'ACTIF'),
    (id_cour2, id_agent, mois_avant_precedent - INTERVAL '1 month', mois_avant_precedent - INTERVAL '1 month' + INTERVAL '1 year', 'GESTION', 12, 'MENSUEL', 'ACTIF');

SELECT id_mandat INTO id_mandat1 FROM contrat_mandat WHERE id_cour = id_cour1;
SELECT id_mandat INTO id_mandat2 FROM contrat_mandat WHERE id_cour = id_cour2;

-- ==============================================================
-- Contrats de location
-- Contrat1 : client1 / Maison A, ACTIF, entré il y a 3 mois
-- Contrat2 : client2 / Maison C, ACTIF, entré il y a 2 mois
-- Contrat3 : client1 / Maison E, entré il y a 2 mois, RESILIE mi-mois courant
-- ==============================================================
INSERT INTO contra_location (id_user, id_maison, date_entree, date_sortie, montant_loyer, statut, type_contrat, depot_garantie, etat_des_lieux_entree, created_at)
VALUES
    (id_client1, id_maison_a, mois_avant_precedent - INTERVAL '1 month', NULL, 150000, 'ACTIF', 'HABITATION', 300000, 'RAS - état correct à l''entrée', NOW()),
    (id_client2, id_maison_c, mois_avant_precedent, NULL, 100000, 'ACTIF', 'HABITATION', 200000, 'RAS - état correct à l''entrée', NOW()),
    (id_client1, id_maison_e, mois_avant_precedent, mois_courant + INTERVAL '14 days', 70000, 'RESILIE', 'HABITATION', 140000, 'RAS - état correct à l''entrée', NOW());

SELECT id_contra_location INTO id_contrat1 FROM contra_location WHERE id_maison = id_maison_a;
SELECT id_contra_location INTO id_contrat2 FROM contra_location WHERE id_maison = id_maison_c;
SELECT id_contra_location INTO id_contrat3 FROM contra_location WHERE id_maison = id_maison_e;

-- ==============================================================
-- Échéances LOCATION — modèle CALENDAIRE (1er du mois, mois occupé + 1)
-- Contrat1 (Maison A) : 2 mois avant = EN_RETARD (jamais payé), mois précédent = PAYE, mois courant = EN_ATTENTE (pas encore due)
-- Contrat2 (Maison C) : mois précédent = PAYE, mois courant = EN_ATTENTE
-- Contrat3 (Maison E, résiliée mi-mois courant) : SEULE l'échéance du mois de résiliation existe (mois courant, EN_ATTENTE)
--   -> aucune échéance future au-delà, simulant le nettoyage de supprimerEcheancesFutures()
-- ==============================================================
INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance, montant_du, montant_paye, statut, created_at)
VALUES
    ('LOCATION', id_contrat1, mois_precedent, 150000, 0, 'EN_RETARD', NOW()),
    ('LOCATION', id_contrat1, mois_courant, 150000, 150000, 'PAYE', NOW()),
    ('LOCATION', id_contrat1, mois_suivant, 150000, 0, 'EN_ATTENTE', NOW()),
    ('LOCATION', id_contrat2, mois_courant, 100000, 100000, 'PAYE', NOW()),
    ('LOCATION', id_contrat2, mois_suivant, 100000, 0, 'EN_ATTENTE', NOW()),
    ('LOCATION', id_contrat3, mois_suivant, 70000, 0, 'EN_ATTENTE', NOW());

SELECT id_echeance INTO id_echeance_a_retard FROM echeance_loyer WHERE entite_echeance_id = id_contrat1 AND date_echeance = mois_precedent;
SELECT id_echeance INTO id_echeance_a_payee FROM echeance_loyer WHERE entite_echeance_id = id_contrat1 AND date_echeance = mois_courant;
SELECT id_echeance INTO id_echeance_c_payee FROM echeance_loyer WHERE entite_echeance_id = id_contrat2 AND date_echeance = mois_courant;

-- ==============================================================
-- Paiements ENTREE (loyers réglés par les locataires) + liaison paiement_echeance
-- ==============================================================
INSERT INTO paiement (date_paiement, montant_paiement, mode_paiement, reference_paiement, sens, user_create)
VALUES
    (mois_courant, 150000, 'MOBILE_MONEY', 'PAY-2026-LOYER-A', 'ENTREE', id_client1),
    (mois_courant, 100000, 'MOBILE_MONEY', 'PAY-2026-LOYER-C', 'ENTREE', id_client2);

SELECT id_paiement INTO id_paiement_loyer_a_precedent FROM paiement WHERE reference_paiement = 'PAY-2026-LOYER-A';
SELECT id_paiement INTO id_paiement_loyer_c_precedent FROM paiement WHERE reference_paiement = 'PAY-2026-LOYER-C';

INSERT INTO paiement_echeance (id_echeance, id_paiement)
VALUES
    (id_echeance_a_payee, id_paiement_loyer_a_precedent),
    (id_echeance_c_payee, id_paiement_loyer_c_precedent);

-- ==============================================================
-- Échéances MANDAT — modèle reversement bailleur (montant_du = NET, commission_deduite = gain agence)
-- Mandat1 (cour1, 10%) : ancienne (2 mois avant, PAYE, déjà reversée) + actuelle (mois précédent, EN_ATTENTE)
-- Mandat2 (cour2, 12%) : actuelle (mois précédent, EN_ATTENTE)
-- ==============================================================

-- Ancienne échéance mandat1, déjà reversée : loyers encaissés 150000, commission 15000, net 135000
INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance, montant_du, montant_paye, commission_deduite, statut, created_at)
VALUES ('MANDAT', id_mandat1, mois_avant_precedent, 135000, 135000, 15000, 'PAYE', NOW());

SELECT id_echeance INTO id_echeance_mandat1_ancienne FROM echeance_loyer
    WHERE entite_echeance_id = id_mandat1 AND entite_echeance_type = 'MANDAT' AND date_echeance = mois_avant_precedent;

INSERT INTO paiement (date_paiement, montant_paiement, mode_paiement, reference_paiement, sens, user_create)
VALUES (mois_precedent, 135000, 'VIREMENT_BANCAIRE', 'REV-2026-MANDAT1-001', 'SORTIE', id_agent);

SELECT id_paiement INTO id_paiement_reversement_mandat1_ancien FROM paiement WHERE reference_paiement = 'REV-2026-MANDAT1-001';

INSERT INTO paiement_echeance (id_echeance, id_paiement)
VALUES (id_echeance_mandat1_ancienne, id_paiement_reversement_mandat1_ancien);

-- Échéance actuelle mandat1, pas encore reversée : loyers encaissés mois précédent 150000, commission 15000, net 135000
INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance, montant_du, montant_paye, commission_deduite, statut, created_at)
VALUES ('MANDAT', id_mandat1, mois_precedent, 135000, 0, 15000, 'EN_ATTENTE', NOW());

-- Échéance actuelle mandat2, pas encore reversée : loyers encaissés mois précédent 100000, commission 12000, net 88000
INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance, montant_du, montant_paye, commission_deduite, statut, created_at)
VALUES ('MANDAT', id_mandat2, mois_precedent, 88000, 0, 12000, 'EN_ATTENTE', NOW());

-- ==============================================================
-- Annonces / Demande / Offre / Contact / Réservation (inchangés)
-- ==============================================================
INSERT INTO annonce (titre, description, type_annonce, date_publication, date_expiration, statut, prix, localisation)
VALUES
    ('Studio à louer - Secteur 12', 'Studio meublé, calme, proche marché', 'LOCATION', NOW(), NOW() + INTERVAL '30 days', 'ACTIVE', 60000, 'Ouagadougou, Secteur 12'),
    ('Villa 3 pièces disponible bientôt', 'Villa moderne, cour clôturée', 'LOCATION', NOW(), NOW() + INTERVAL '15 days', 'ACTIVE', 150000, 'Ouagadougou, Secteur 12');

INSERT INTO demande (nom_complet, email, telephone, type_bien, localisation_souhaite, budget_max, description, date_demande, statut)
VALUES ('Sana Awa', 'sana.awa@test.com', '70123456', 'Appartement', 'Ouagadougou', 100000, 'Recherche 2 pièces proche université', NOW(), 'EN_ATTENTE');

INSERT INTO offre (nom_complet, email, telephone, type_offre, titre, description, adresse, date_offre, statut)
VALUES ('Traore Boureima', 'traore.b@test.com', '70654321', 'MAISON', 'Cour à confier en gestion', 'Cour de 4 maisons, secteur 15, bon état', 'Secteur 15, Ouagadougou', NOW(), 'ACTIVE');

INSERT INTO contact (nom_complet, email, telephone, sujet, message, date_envoi, statut)
VALUES ('Zongo Salif', 'zongo.salif@test.com', '70999888', 'Question sur une annonce', 'Bonjour, le studio du secteur 12 est-il toujours disponible ?', NOW(), 'NON_LU');

INSERT INTO reservation_maison (id_user, id_maison, date_debut, date_fin, statut)
VALUES (id_client1, id_maison_b, NOW() + INTERVAL '5 days', NOW() + INTERVAL '10 days', 'EN_ATTENTE');

-- ==============================================================
-- Locations Biens & Services — scénarios A à E (inchangés, cohérents avec la session précédente)
-- ==============================================================

-- Scénario A — Demande EN_ATTENTE
INSERT INTO location_bien_service (id_user, id_bien_service, destination, date_debut, date_fin, duree, montant_total, statut, user_create, created_at)
VALUES (id_client1, id_bien_vehicule, 'Déplacement Bobo-Dioulasso', NOW() + INTERVAL '2 days', NOW() + INTERVAL '5 days', 3, 105000, 'EN_ATTENTE', id_client1, NOW());

-- Scénario B — ACTIF avec paiement initial
INSERT INTO paiement (date_paiement, montant_paiement, mode_paiement, reference_paiement, sens, user_create)
VALUES (NOW(), 45000, 'MOBILE_MONEY', 'PAY-2026-0002', 'ENTREE', id_agent);

INSERT INTO location_bien_service (id_user, id_bien_service, destination, date_debut, date_fin, duree, montant_total, statut, user_create, user_update, created_at)
VALUES (id_client2, id_bien_sono, 'Événement mariage', NOW() - INTERVAL '2 days', NOW() + INTERVAL '1 day', 3, 45000, 'ACTIF', id_agent, id_agent, NOW());

INSERT INTO paiement_location_bien_service (id_location_bien_service, id_paiement, type_paiement)
VALUES ((SELECT id_location_bien_service FROM location_bien_service WHERE destination = 'Événement mariage'),
        (SELECT id_paiement FROM paiement WHERE reference_paiement = 'PAY-2026-0002'), 'INITIAL');

-- Scénario C — ACTIF prolongée
INSERT INTO paiement (date_paiement, montant_paiement, mode_paiement, reference_paiement, sens, user_create)
VALUES (NOW() - INTERVAL '5 days', 140000, 'MOBILE_MONEY', 'PAY-2026-0003', 'ENTREE', id_agent);

INSERT INTO location_bien_service (id_user, id_bien_service, destination, date_debut, date_fin, duree, montant_total, statut, user_create, user_update, created_at)
VALUES (id_client1, id_bien_vehicule, 'Mission terrain prolongée', NOW() - INTERVAL '5 days', NOW() + INTERVAL '2 days', 7, 245000, 'ACTIF', id_agent, id_agent, NOW());

INSERT INTO paiement_location_bien_service (id_location_bien_service, id_paiement, type_paiement)
VALUES ((SELECT id_location_bien_service FROM location_bien_service WHERE destination = 'Mission terrain prolongée'),
        (SELECT id_paiement FROM paiement WHERE reference_paiement = 'PAY-2026-0003'), 'INITIAL');

INSERT INTO paiement (date_paiement, montant_paiement, mode_paiement, reference_paiement, sens, user_create)
VALUES (NOW(), 105000, 'ESPECES', 'PAY-2026-0004', 'ENTREE', id_agent);

INSERT INTO paiement_location_bien_service (id_location_bien_service, id_paiement, type_paiement)
VALUES ((SELECT id_location_bien_service FROM location_bien_service WHERE destination = 'Mission terrain prolongée'),
        (SELECT id_paiement FROM paiement WHERE reference_paiement = 'PAY-2026-0004'), 'PROLONGATION');

-- Scénario D — ACTIF raccourcie + remboursement partiel
INSERT INTO paiement (date_paiement, montant_paiement, mode_paiement, reference_paiement, sens, user_create)
VALUES (NOW() - INTERVAL '3 days', 210000, 'MOBILE_MONEY', 'PAY-2026-0005', 'ENTREE', id_agent);

INSERT INTO location_bien_service (id_user, id_bien_service, destination, date_debut, date_fin, duree, montant_total, statut, user_create, user_update, created_at)
VALUES (id_client2, id_bien_vehicule, 'Location raccourcie test', NOW() - INTERVAL '3 days', NOW() + INTERVAL '1 day', 4, 140000, 'ACTIF', id_agent, id_agent, NOW());

INSERT INTO paiement_location_bien_service (id_location_bien_service, id_paiement, type_paiement)
VALUES ((SELECT id_location_bien_service FROM location_bien_service WHERE destination = 'Location raccourcie test'),
        (SELECT id_paiement FROM paiement WHERE reference_paiement = 'PAY-2026-0005'), 'INITIAL');

-- Remboursement : lié à un NOUVEAU paiement SORTIE (pas au paiement d'origine) — corrige l'erreur de l'ancien seed
INSERT INTO paiement (date_paiement, montant_paiement, mode_paiement, reference_paiement, sens, user_create)
VALUES (NOW(), 40000, 'ESPECES', 'RMB-2026-0001', 'SORTIE', id_agent);

SELECT id_paiement INTO id_paiement_remboursement FROM paiement WHERE reference_paiement = 'RMB-2026-0001';

INSERT INTO remboursement (entite_type, id_paiement, entite_id, motif, user_create)
VALUES ('LOCATION_BIEN_SERVICE', id_paiement_remboursement,
        (SELECT id_location_bien_service FROM location_bien_service WHERE destination = 'Location raccourcie test'),
        'Raccourcissement de durée', id_agent);


INSERT INTO parametre_systeme (cle, valeur, type_valeur, description) VALUES
    ('TOLERANCE_LOCATION_JOURS', '4', 'ENTIER', 'Nombre de jours de tolérance avant qu''un loyer en retard soit marqué EN_RETARD'),
    ('TOLERANCE_MANDAT_JOURS', '10', 'ENTIER', 'Nombre de jours de tolérance avant qu''un reversement bailleur soit considéré en retard'),
    ('MEDIAS_RETENTION_JOURS', '30', 'ENTIER', 'Délai avant purge définitive d''un média supprimé'),
    ('MEDIAS_MAX_FICHIERS_PAR_LOT', '10', 'ENTIER', 'Nombre maximum de fichiers par envoi groupé'),
    ('PENALITE_RETARD_MONTANT', '2000', 'DECIMAL', 'Montant fixe appliqué une seule fois par échéance de loyer passée en retard');

-- Scénario E — ANNULEE
INSERT INTO location_bien_service (id_user, id_bien_service, destination, date_debut, date_fin, duree, montant_total, statut, user_create, user_update, created_at)
VALUES (id_client1, id_bien_sono, 'Demande annulée', NOW() + INTERVAL '10 days', NOW() + INTERVAL '12 days', 2, 30000, 'ANNULE', id_client1, id_agent, NOW());

RAISE NOTICE 'Seed complet terminé avec succès.';

END $$;

-- ==============================================================
-- Vérification finale
-- ==============================================================
SELECT 'Seed terminé avec succès !' AS status;
SELECT
    (SELECT COUNT(*) FROM users) AS nb_users,
    (SELECT COUNT(*) FROM cour) AS nb_cours,
    (SELECT COUNT(*) FROM maison) AS nb_maisons,
    (SELECT COUNT(*) FROM contrat_mandat) AS nb_mandats,
    (SELECT COUNT(*) FROM contra_location) AS nb_contrats_location,
    (SELECT COUNT(*) FROM echeance_loyer WHERE entite_echeance_type = 'LOCATION') AS nb_echeances_location,
    (SELECT COUNT(*) FROM echeance_loyer WHERE entite_echeance_type = 'MANDAT') AS nb_echeances_mandat,
    (SELECT COUNT(*) FROM remboursement) AS nb_remboursements,
    (SELECT COUNT(*) FROM location_bien_service) AS nb_locations_bien_service,
    (SELECT COUNT(*) FROM annonce) AS nb_annonces;