# Rapport de pré-recette du 04/11/2026 (tranche 1 : automatique, droits d'accès, sécurité de base)

Environnement : pile Docker locale (développement, jeu de données de test, HTTPS auto-signé). Seul le backend a été exercé ; les parcours dans le navigateur ne sont pas encore faits.

## Résultat global

Aucun défaut bloquant. Un point de procédure corrigé, trois points à confirmer ou à traiter avant la préproduction.

## Ce qui a été vérifié et réussi

| Contrôle | Résultat |
|---|---|
| Suite de tests du backend (JDK 21, conteneurs jetables) | **116 tests, 0 échec** |
| Routes sensibles sans connexion (utilisateurs, mandats, contrats, paiements, notifications, Swagger, Actuator) | 401 partout |
| Matrice des droits par rôle sur 26 routes (anonyme, client, bailleur, agent, admin) | Conforme aux annotations ; `/api/parametres` et `/api/journaux` réservés à l'admin |
| Accès par identifiant (contrats de location, échéances, réservations, mandats) testé avec 15 comptes clients et bailleurs | Chaque contrat n'est lisible que par son client et son bailleur |
| Limite de connexion | Blocage (429) au 6e mauvais essai de suite |
| Cookies | HttpOnly, SameSite=Strict, refresh limité à `/api/auth/refresh-token` |
| CORS depuis une origine étrangère | Refusé (403) |
| Erreurs (JSON invalide, identifiant absurde) | Messages propres ; le détail technique n'apparaît qu'avec le profil `dev` |
| Injection SQL basique sur la recherche publique | Pas d'erreur (200) |
| Redirection HTTP vers HTTPS | 301 ; accès par IP directe refusé |

## Points relevés

1. **Supervision en HEAD (corrigé dans la procédure).** `/api/stats/public` répond 200 en GET mais 401 en HEAD. Un outil de supervision réglé en HEAD annoncerait une fausse panne. Procédure mise à jour.
2. **Un agent voyait tous les comptes** dans `/api/admin/users` (10 clients, 5 bailleurs, 3 agents, 1 administrateur), avec date de naissance et téléphones. **Corrigé le 04/11/2026** : l'administrateur est invisible pour un agent (liste, recherche et fiche, 403), les fiches des collègues agents sont réduites à l'identité et à l'e-mail (champs sensibles à `null`), les clients et bailleurs restent complets. Les agents restent listés car le formulaire des biens-services leur fait choisir un gestionnaire parmi eux. L'administrateur voit tout. Couvert par `VisibiliteUtilisateursParAgentIntegrationTest` (6 tests).
3. **Cookies sans `Secure` en local** : normal en développement (`JWT_COOKIE_SECURE=false`). En préproduction et en production, la valeur doit être `true` ; à contrôler dans la recette.
4. **Compte `client7@gestimmo.test` verrouillé** quelques minutes par le test de limite de connexion (effet de mon test, sans conséquence durable).

## Non couvert (tranches suivantes)

- Parcours dans le navigateur (connexion avec 2FA, annonces, mandats, paiements, documents PDF, captures).
- Écritures par rôle : un client ou un bailleur ne doit pas pouvoir modifier ni supprimer la ressource d'un autre (PUT, PATCH, DELETE).
- Téléversement de fichiers (types refusés, taille, accès aux fichiers privés).
- Flux financiers de bout en bout : paiement, quittance, reversement, rapport mensuel.
- Équipement : mobile, autres navigateurs, délivrabilité des e-mails, charge.

## Tranche 2 : parcours navigateur et écritures entre utilisateurs (2026-11-04)

**Parcours navigateur** (Chrome headless, bureau 1366 px et mobile 390 px, 102 pages : public, client, bailleur, agent, admin) : aucune erreur d'API ni de JavaScript, aucune page vide. Un défaut trouvé et corrigé : la barre d'onglets de `/espace-admin/utilisateurs` débordait sur mobile (elle défile maintenant). Non couvert : pages détail avec identifiant.

**Écritures par rôle** (51 endpoints PUT/PATCH/DELETE, id inexistant, visiteur / client / bailleur / agent / admin) : le visiteur reçoit toujours 401, les rôles non autorisés reçoivent 403, les rôles autorisés 404, aucune réponse 2xx ni 5xx. Note : la validation du corps (400) passe avant le contrôle de rôle, ce qui ne change aucun état. Re-testé avec des corps valides : 403 conforme.

**Écritures croisées avec de vrais ids** (13 cas) : un client ne peut ni annuler ni confirmer la réservation ou la location d'un autre, ni résilier ou terminer un contrat de location ; un bailleur ne peut ni résilier, activer, modifier ni supprimer le mandat d'un autre bailleur, ni confirmer un virement. Tout renvoie 403. Aucun écart.

## Tranche 3 : téléversements (2026-11-04)

**Envoi par un agent** (`POST /api/medias`, sur une maison) : refusés comme attendu une page HTML renommée en .jpg, un exécutable renommé en .png, un SVG (script), un fichier vide (400), une image de 30000 x 30000 px déclarée (bombe de décompression, 413) et une image de 6 Mo (limite 5 Mo, 413). Le nom de fichier envoyé (`../../etc/passwd.png`) est ignoré : la clé de stockage est un UUID avec l'extension déduite du type réel. Sans connexion : 401.

**Fichier polyglotte** (PNG valide suivi de HTML) : accepté, mais servi en `image/png` avec `X-Content-Type-Options: nosniff`, y compris via nginx. Aucun risque d'exécution dans le navigateur.

**Dépôt public de photos sur une offre** (`POST /api/public/offres/{id}/medias`) : sans jeton ou avec un mauvais jeton 403 ; jeton d'une autre offre inutilisable ; images seules (HTML et exécutable refusés dans un lot mixte, les bonnes photos du lot passent) ; 10 fichiers maximum par envoi (400) ; 10 photos maximum par offre (400 au-delà). Les médias d'une offre sont invisibles pour un visiteur (400 sur la liste, 404 sur le fichier) et visibles pour un agent.

**Point relevé (mineur, agent seulement)** : un agent peut téléverser un média sur un identifiant d'entité inexistant (maison 999999 : 201). Cela crée un média orphelin, sans effet visible ni risque d'accès. **Corrigé** : le service de médias vérifie désormais que l'entité existe (404 sinon), avec un test de non-régression. Le seul média orphelin déjà en base de développement (n° 4, déjà supprimé logiquement) sera purgé par la tâche quotidienne.

Les médias créés pour les tests ont été supprimés (suppression logique). Deux offres de test restent en base de développement (ids 183 et suivante, "Test Recette").

**Reste à faire** : flux financiers de bout en bout (paiement, quittance, reversement, rapport mensuel).

## Tranche 4 : flux financiers (2026-11-04)

**Conforme.** Enregistrement d'un paiement réservé aux agents et administrateurs (un client reçoit 403, même sur sa propre échéance ; un visiteur 401). Paiement partiel puis solde : l'échéance reste en attente jusqu'au solde, puis passe à PAYE. Surpaiement refusé (400), échéance inexistante 404, paiement d'une échéance déjà soldée refusé. **Quatre envois simultanés du même paiement : un seul accepté (201), les trois autres refusés (409)**, aucun double encaissement. Quittance et reçu en PDF : accessibles à l'agent et au locataire concerné, 403 pour un autre client, 401 pour un visiteur. Confirmation de virement sur une échéance qui n'est pas de type MANDAT refusée (409), calcul des reversements interdit au bailleur (403), date invalide 400. Rapport mensuel : réservé à l'administrateur (agent, bailleur, client 403), PDF généré pour un mois valide, 400 pour un mois futur. La base interdit la suppression des lignes de paiement et d'échéance liée (trigger de preuve financière).

**Défauts trouvés et corrigés** (agent ou administrateur seulement, intégrité des données) :
- un paiement de montant négatif ou nul était enregistré (ligne parasite dans la table des paiements, sans échéance liée) ;
- un mode de paiement vide était accepté ;
- une date de paiement en 2099 était acceptée ;
- une échéance donnée deux fois dans la liste renvoyait un 404 trompeur : les doublons sont maintenant ignorés.

Correctif : montant strictement positif, mode non vide, date non future (tolérance 5 minutes), doublons retirés. Nouveau test dans `EcritureMetierIntegrationTest` (16 tests verts), backend reconstruit et revérifié (400 sur les quatre cas).

**Non couvert** : calcul réel des reversements et confirmation d'un virement (écritures irréversibles en base, couvertes par les tests automatiques), rapport mensuel avec contenu chiffré vérifié à l'œil, envoi des e-mails.

**Traces de test laissées en base de développement** (non supprimables, trigger) : paiements 36 à 42, dont un de -5000 et un de 0 sans échéance liée ; échéance 36 partiellement payée (10 300), échéance 37 soldée. Sans effet en production : la base de production repart du seed.
