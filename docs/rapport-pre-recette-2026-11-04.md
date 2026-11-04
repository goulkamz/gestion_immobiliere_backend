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
