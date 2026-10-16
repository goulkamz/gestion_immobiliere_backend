-- Rattrapage : notifications creees avant l'ajout du ciblage (?cible=<id>) ; l'identifiant visee se deduit de la cle d'alerte.
UPDATE notification n SET lien = n.lien || '?cible=' || a.id_contrat_location
FROM avenant a
WHERE n.cle_alerte = 'AVENANT:' || a.id_avenant AND a.id_contrat_location IS NOT NULL
  AND n.lien IN ('/espace-client/contrats', '/espace-bailleur/contrats');

UPDATE notification n SET lien = n.lien || '?cible=' || a.id_mandat
FROM avenant a
WHERE n.cle_alerte = 'AVENANT:' || a.id_avenant AND a.id_mandat IS NOT NULL AND n.lien = '/espace-bailleur/mandats';

UPDATE notification SET lien = lien || '?cible=' || split_part(cle_alerte, ':', 2)
WHERE lien NOT LIKE '%cible=%'
  AND ((cle_alerte LIKE 'LOCATION_ECHEANCE:%' OR cle_alerte LIKE 'LOCATION_RETARD:%') AND lien = '/espace-agent/locations-biens-services'
    OR cle_alerte LIKE 'CLIENT_LOCATION_FIN:%' AND lien = '/espace-client/locations'
    OR (cle_alerte LIKE 'ECHEANCE_PROCHE:%' OR cle_alerte LIKE 'ECHEANCE_RETARD:%') AND lien = '/espace-client/echeances');

-- Recu de loyer : la premiere echeance couverte par le paiement
UPDATE notification n SET lien = '/espace-client/echeances?cible=' || (
    SELECT min(pe.id_echeance) FROM paiement_echeance pe WHERE pe.id_paiement = split_part(n.cle_alerte, ':', 2)::int)
WHERE n.cle_alerte LIKE 'PAIEMENT_RECU:%' AND n.lien = '/espace-client/recus'
  AND EXISTS (SELECT 1 FROM paiement_echeance pe WHERE pe.id_paiement = split_part(n.cle_alerte, ':', 2)::int);
