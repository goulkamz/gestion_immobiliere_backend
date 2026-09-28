# API Gestion Immobilière — référence frontend

Ce document décrit l'intégralité des endpoints REST exposés par le backend Spring Boot, à destination du frontend React + TypeScript développé séparément. Généré par lecture directe du code source (`modules/*/apis/*API.java`, `modules/*/controllers/*Controller.java`, DTOs, enums) le 2026-09-28.

## 1. Généralités

### Base URL

- Local (via nginx, TLS auto-signé) : `https://api.gestimmo.localhost`
- Local (backend direct, sans nginx) : `http://localhost:8080`
- Production : `https://api.<domaine>` (configuré via `API_DOMAIN`)

Toutes les routes ci-dessous sont relatives à cette base (ex. `POST /api/auth/signin` → `https://api.gestimmo.localhost/api/auth/signin`).

### Authentification par cookies (pas de header Authorization)

L'authentification est stateless JWT mais transportée exclusivement par cookies httpOnly (`access_token`, `refresh_token`), `SameSite=Strict`, `Secure`. Le frontend **ne stocke jamais de token en JS** — le navigateur envoie les cookies automatiquement à chaque requête vers le même domaine (ou sous-domaine autorisé par CORS).

```typescript
// fetch
fetch(`${API_BASE_URL}/api/users/me`, { credentials: 'include' });

// axios
axios.create({ baseURL: API_BASE_URL, withCredentials: true });
```

CORS est préconfiguré (`WebSecurityConfig`) pour `http://localhost:3000` et `http://localhost:5173` (`app.cors.allowed-origins`), avec `allowCredentials(true)`.

Cycle de vie : `POST /api/auth/signin` pose les cookies → requêtes authentifiées automatiques → `POST /api/auth/refresh-token` quand `access_token` expire (401) → `POST /api/auth/logout` / `logout-all` pour révoquer.

Routes publiques (`permitAll`, définies dans `WebSecurityConfig`) : tout `/api/auth/**`, tout `/api/public/**`, et `GET /api/stats/public` + `/api/stats/public/**`. Tout le reste exige un cookie `access_token` valide, et au-delà de l'authentification, des règles `@PreAuthorize` par rôle s'appliquent au niveau des controllers (parfois plus restrictives que ce qui figure sur l'interface `*API.java` — toujours vérifier le controller).

### 4 rôles

```typescript
type Role = 'ROLE_CLIENT' | 'ROLE_BAILLEUR' | 'ROLE_SECRETAIRE' | 'ROLE_AGENT' | 'ROLE_SG' | 'ROLE_DIRECTEUR' | 'ROLE_DG' | 'ROLE_PDG' | 'ROLE_ADMIN';
```
`ERole` définit 9 valeurs en base, mais la logique métier documentée dans le cahier des charges et dans le code (`@PreAuthorize`) n'utilise concrètement que 4 : `ROLE_CLIENT` (locataire, rôle par défaut à l'inscription), `ROLE_BAILLEUR` (propriétaire), `ROLE_AGENT` (agent/secrétaire), `ROLE_ADMIN`. Les rôles `ROLE_SECRETAIRE`, `ROLE_SG`, `ROLE_DIRECTEUR`, `ROLE_DG`, `ROLE_PDG` ont été ajoutés intentionnellement en anticipation d'une évolution future de la hiérarchie interne de l'agence (confirmé par le développeur, pas un oubli) — ils existent dans l'enum mais n'apparaissent dans aucun `@PreAuthorize` actuel. Traitez-les comme réservés/non utilisés côté frontend pour l'instant (pas de vue à construire pour eux tant qu'aucune règle d'autorisation ne les cible).

### Enveloppe de réponse succès

Construite par `utils/BuildSuccessResponse.buildSuccessResponse(status, message, code, data)` :

```typescript
interface ApiResponse<T = unknown> {
  success: true;
  message: string;
  code: string;       // ex: "LOCATION_FOUND", "PAIEMENT_FOUND", ...
  timestamp: string;  // ISO-8601 Instant
  data?: T;            // absent (pas juste null) si data était null côté serveur
}
```

**Exceptions à l'enveloppe** (confirmées dans le code, à traiter à part côté client) :
- `POST /api/contrats-location` (création d'un contrat de location) retourne directement un `ContratLocationResponseDTO` en JSON, **sans enveloppe** `{success, data, ...}`.
- Tous les endpoints `/api/documents/**` retournent un flux binaire `application/pdf` (`Content-Disposition: attachment`), pas de JSON.
- `GET /api/public/medias/{id}/fichier` et `/miniature` retournent le binaire de l'image (pas de JSON).

### Enveloppe d'erreur

Construite par `exceptions/GlobalExceptionHandler` :

```typescript
interface ApiError {
  success: false;
  message: string;
  code: string;              // ex: "VALIDATION_ERROR", "EMAIL_ALREADY_EXISTS", "MANDAT_ACTIF_EXISTANT"...
  timestamp: string;
  debug_message?: string;    // uniquement si profil Spring actif = "dev"
  exception_type?: string;   // idem, uniquement en dev
}

// Cas particulier : erreurs de validation de champ (400, MethodArgumentNotValidException)
interface ApiValidationError {
  success: false;
  message: string;            // "Erreur de validation des champs"
  code: 'VALIDATION_ERROR';
  timestamp: string;
  errors: Record<string, string>; // { "champ": "message d'erreur" }
}
```

Table des codes d'erreur métier observés dans `GlobalExceptionHandler` (non exhaustive pour les erreurs génériques Spring Security type `INVALID_CREDENTIALS`, `ACCOUNT_DISABLED`, etc.) :

| HTTP | code | Déclenché par |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Échec de validation Bean Validation (`@Valid`) |
| 400 | `INVALID_ARGUMENT` | `IllegalArgumentException` |
| 400 | `RUNTIME_ERROR` | `RuntimeException` "brute" (message métier ad hoc) |
| 400 | `INVALID_EMAIL`, `INVALID_PASSWORD`, `ROLE_NOT_FOUND`, `INVALID_TOKEN`, `TOKEN_ALREADY_USED`, `TOKEN_EXPIRED`, `SAME_PASSWORD` | Auth / reset password |
| 400 | `MONTANT_PAIEMENT_INVALIDE`, `DATE_EXPIRATION_INVALIDE` | Règles métier paiements / annonces |
| 400 | `FORMAT_MEDIA_INVALIDE` | Média hors JPEG/PNG/WEBP |
| 401 | `INVALID_CREDENTIALS`, `USER_NOT_FOUND`, `NOT_AUTHENTICATED`, `INVALID_REFRESH_TOKEN` | Authentification |
| 403 | `ACCOUNT_DISABLED`, `ACCOUNT_LOCKED`, `ACCOUNT_EXPIRED`, `CREDENTIALS_EXPIRED`, `INSUFFICIENT_PRIVILEGES`, `ACCESS_DENIED`, `CANNOT_DEACTIVATE_SELF` | Compte / permissions |
| 404 | code porté par l'exception (`ResourceNotFoundException.getCode()`), ex. `USER_NOT_FOUND` | Ressource introuvable |
| 409 | `CODE_ALREADY_EXISTS`, `EMAIL_ALREADY_EXISTS`, `INVALID_STATUT_TRANSITION`, `MANDAT_ACTIF_EXISTANT`, `MAISON_INDISPONIBLE`, `ECHEANCE_DEJA_PAYEE`, `CONFLIT_RESERVATION`, `INVALID_STATE_TRANSITION` | Conflits/règles métier (RG2, RG3...) |
| 413 | `TAILLE_MEDIA_EXCESSIVE`, `IMAGE_DIMENSIONS_EXCESSIVE` | Média trop volumineux/dimensions excessives |
| 429 | `TOO_MANY_REQUESTS` | Rate limiting (login, etc.) |
| 500 | `INTERNAL_ERROR`, `INTERNAL_AUTH_ERROR`, `MEDIA_STORAGE_ERROR` | Erreurs techniques non anticipées |
| 503 | `AUTH_SERVICE_UNAVAILABLE` | Service d'auth indisponible |

### Pagination

Les listes utilisent `org.springframework.data.domain.Pageable` en query params : `?page=0&size=20&sort=champ,asc`. La réponse suit le format Spring Boot moderne dans `data` :

```typescript
interface PagedResponse<T> {
  content: T[];
  page: {
    size: number;
    number: number;         // page courante, 0-indexé
    totalElements: number;
    totalPages: number;
  };
}
```

---

## 2. Authentification (`/api/auth`) — `AuthentificationAPI`

Toutes publiques (`permitAll`). Contrôleur : `UserController`.

### POST /api/auth/signin
Connexion. Pose les cookies `access_token`/`refresh_token`.
Rôle : public
Body :
```typescript
interface AuthenticateRequest {
  email: string;    // requis — alias JSON acceptés : "username", "userName", "email", "telephone"
  password: string; // requis
}
```
Réponse (data) : `UserInfoResponse` (voir ci-dessous). Codes : 401 `INVALID_CREDENTIALS`/`USER_NOT_FOUND`, 403 `ACCOUNT_DISABLED`/`ACCOUNT_LOCKED`, 429 `TOO_MANY_REQUESTS` (rate limiting par IP, Redis).

### POST /api/auth/signup
Inscription (rôle client par défaut), déclenche l'envoi d'un code d'activation par email.
Rôle : public
Body :
```typescript
interface CreateUserRequest {
  nom: string;               // requis, max 254
  prenom: string;            // requis, max 254
  sexe?: string;              // 1 caractère M/F
  email: string;              // requis, format email, max 254
  password: string;           // requis, 6-120 car., maj+min+chiffre+caractère spécial ASCII
  dateNaissance: string;      // requis, format yyyy-MM-dd, doit être dans le passé
  telephone: string;          // requis, format international/local
  telephone1?: string;         // secondaire, optionnel
  idRole: number;             // requis — id du rôle
}
```
Réponse (data) : probablement un message de confirmation / DTO utilisateur créé (non totalement confirmé — le service `UserService.createUser` n'a pas été lu en détail). Codes : 400 `VALIDATION_ERROR`, 409 `EMAIL_ALREADY_EXISTS`.

### POST /api/auth/activation
Active le compte via le code reçu par email.
Rôle : public
Body : `{ code: string }` (requis, alias JSON `code`).
Codes : 400 `INVALID_TOKEN`/`TOKEN_EXPIRED`/`TOKEN_ALREADY_USED`.

### POST /api/auth/resend-code
Renvoie le code d'activation.
Rôle : public. Body : `{ email: string }` (requis).

### POST /api/auth/forgot-password
Démarre une réinitialisation de mot de passe (envoi d'un token par email, valable ~1h, 3 tentatives max).
Rôle : public. Body : `{ email: string }` (requis, format email).

### POST /api/auth/reset-password
Finalise la réinitialisation.
Rôle : public
Body :
```typescript
interface ResetPasswordRequest {
  code: string;        // requis
  newPassword: string; // requis, 6-120 car., même règle de complexité que le password d'inscription
}
```
Codes : 400 `INVALID_TOKEN`/`TOKEN_EXPIRED`/`TOKEN_ALREADY_USED`/`SAME_PASSWORD`.

### POST /api/auth/forgot-password/resend
Renvoie le token de réinitialisation.
Rôle : public. Body : `{ email: string }` (requis).

### POST /api/auth/refresh-token
Rafraîchit `access_token` à partir du cookie `refresh_token`. Pas de body (lu depuis les cookies).
Rôle : public (mais nécessite le cookie `refresh_token` valide). Codes : 401 `INVALID_REFRESH_TOKEN`.

### POST /api/auth/logout
Révoque le refresh token courant, efface les cookies.
Rôle : public (agit sur la session courante via cookies).

### POST /api/auth/logout-all
Révoque tous les refresh tokens de l'utilisateur connecté (toutes sessions/appareils).
Rôle : public au sens Security (mais nécessite d'être connecté pour avoir un effet utile).

```typescript
interface UserInfoResponse {
  username: string;
  accessToken: string;   // présent dans le payload JSON même si le cookie fait foi ; utile pour mobile
  refreshToken: string;  // idem, pour mobile
  expiresIn: number;     // secondes
  roles: string[];       // ex: ["ROLE_CLIENT"]
}
```

---

## 3. Profil utilisateur (`/api/users`) — `UserProfileAPI`

Contrôleur : `UserProfileController`, `@PreAuthorize("isAuthenticated()")` au niveau classe.

### GET /api/users/me
Récupère le profil de l'utilisateur connecté.
Rôle requis : authentifié (tous rôles)
Réponse (data) :
```typescript
interface ProfileResponse {
  idUser: number;
  email: string;
  nom: string;
  prenom: string;
  sexe: string | null;
  telephone: string | null;
  telephone1: string | null;
  dateNaissance: string; // yyyy-MM-dd
  role: string;
}
```

### PUT /api/users/me
Met à jour son propre profil. Email et rôle volontairement exclus (non modifiables par l'utilisateur, F4).
Rôle requis : authentifié
Body :
```typescript
interface UpdateProfileRequest {
  nom?: string;
  prenom?: string;
  telephone?: string;
  telephone1?: string;
  dateNaissance?: string; // yyyy-MM-dd
}
```
Réponse (data) : `ProfileResponse`.

---

## 4. Administration des utilisateurs (`/api/admin/users`) — `UserAdminAPI`

Contrôleur : `UserAdminController`, `@PreAuthorize("hasRole('ADMIN')")` au niveau classe — **tous les endpoints ci-dessous sont réservés à ROLE_ADMIN**.

### GET /api/admin/users
Liste paginée des utilisateurs.
Query : `role?` (`ERole`), pagination.
Réponse (data) : `PagedResponse<UserAdminResponse>` avec :
```typescript
interface UserAdminResponse {
  idUser: number;
  email: string;
  nom: string;
  prenom: string;
  sexe: string | null;
  telephone: string | null;
  telephone1: string | null;
  dateNaissance: string;
  flagActif: boolean;
  role: string;
  dateCreate: string;       // ISO LocalDateTime
  dateLastLogin: string | null;
}
```

### GET /api/admin/users/{id}
Détail d'un utilisateur. Réponse : `UserAdminResponse`. Codes : 404.

### POST /api/admin/users
Crée un utilisateur (admin peut créer n'importe quel rôle).
Body :
```typescript
interface CreateUserByAdminRequest {
  email: string;      // requis
  password: string;   // requis
  nom: string;         // requis
  prenom: string;      // requis
  sexe?: string;
  telephone?: string;
  telephone1?: string;
  dateNaissance?: string;
  role: EnumRole;      // requis — ERole
}
```
Réponse : `UserAdminResponse`. Codes : 409 `EMAIL_ALREADY_EXISTS`.

### PUT /api/admin/users/{id}
Met à jour un utilisateur (hors mot de passe/rôle, qui ont leurs propres endpoints dédiés pour la traçabilité).
Body :
```typescript
interface UpdateUserByAdminRequest {
  nom?: string;
  prenom?: string;
  sexe?: string;
  telephone?: string;
  telephone1?: string;
  dateNaissance?: string;
}
```

### PATCH /api/admin/users/{id}/role
Change le rôle d'un utilisateur. Body : `{ role: EnumRole }` (requis).

### PATCH /api/admin/users/{id}/status
Active/désactive un compte. Body : `{ flagActif: boolean }` (requis). Codes : 403 `CANNOT_DEACTIVATE_SELF` (un admin ne peut pas se désactiver lui-même).

### DELETE /api/admin/users/{id}
Supprime (soft delete) un utilisateur. Codes : 404, 403 `CANNOT_DEACTIVATE_SELF` potentiellement applicable selon implémentation service.

---

## 5. Localisation (`pays` → `ville` → `secteur`)

Hiérarchie F-something du cahier des charges. Chaque niveau a une API admin (écriture) et une API publique (lecture, utilisée par les formulaires publics et le catalogue).

### Pays

#### GET /api/public/pays
Liste paginée. Rôle : public. Query : pagination. Réponse (data) : `PagedResponse<PaysResponse>`.
#### GET /api/public/pays/{id}
Détail. Rôle : public. Réponse : `PaysResponse`. Codes : 404.
#### POST /api/pays
Crée un pays. Rôle requis : ROLE_ADMIN ou ROLE_AGENT (le controller `PaysAdminController` autorise `hasAnyRole('ADMIN','AGENT')` sur create/update malgré `@PreAuthorize("hasRole('ADMIN')")` au niveau de l'interface — le controller prévaut).
Body : `{ codePays: string; nomPays: string }` (les deux requis).
#### PUT /api/pays/{id}
Met à jour. Rôle requis : ROLE_ADMIN, ROLE_AGENT. Body : `{ codePays?: string; nomPays?: string }`.
#### DELETE /api/pays/{id}
Supprime. Rôle requis : ROLE_ADMIN uniquement.

```typescript
interface PaysResponse { idPays: number; codePays: string; nomPays: string; }
```

### Ville

#### GET /api/public/villes
Rôle : public. Query : `idPays?`, pagination. Réponse (data) : `PagedResponse<VilleResponse>`.
#### GET /api/public/villes/{id}
Rôle : public. Réponse : `VilleResponse`. Codes : 404.
#### POST /api/villes
Rôle requis : ROLE_ADMIN, ROLE_AGENT (même remarque que Pays — controller autorise plus large que l'interface). Body : `{ idPays: number; codeVille: string; nomVille: string }` (tous requis).
#### PUT /api/villes/{id}
Rôle requis : ROLE_ADMIN, ROLE_AGENT. Body : `{ idPays?: number; codeVille?: string; nomVille?: string }`.
#### DELETE /api/villes/{id}
Rôle requis : ROLE_ADMIN uniquement.

```typescript
interface VilleResponse { idVille: number; codeVille: string; nomVille: string; idPays: number; nomPays: string; }
```

### Secteur

#### GET /api/public/secteurs
Rôle : public. Query : `idVille?`, pagination. Réponse (data) : `PagedResponse<SecteurResponse>`.
#### GET /api/public/secteurs/{id}
Rôle : public. Réponse : `SecteurResponse`. Codes : 404.
#### POST /api/secteurs
Rôle requis : ROLE_ADMIN, ROLE_AGENT. Body : `{ idVille: number; codeSecteur: string; nomSecteur: string }` (tous requis).
#### PUT /api/secteurs/{id}
Rôle requis : ROLE_ADMIN, ROLE_AGENT. Body : `{ idVille?: number; codeSecteur?: string; nomSecteur?: string }`.
#### DELETE /api/secteurs/{id}
Rôle requis : ROLE_ADMIN uniquement.

```typescript
interface SecteurResponse { idSecteur: number; codeSecteur: string; nomSecteur: string; idVille: number; nomVille: string; }
```

---

## 6. Biens (cour, maison, catégorie, bien/service, location de bien/service)

### Cour (`/api/cours`) — `CourAPI`/`CourController`

Une cour peut contenir plusieurs maisons. Un bailleur ne crée jamais directement une cour (c'est un agent qui la crée après acceptation de la proposition).

#### GET /api/cours
Liste paginée. Rôle requis : ROLE_AGENT, ROLE_ADMIN, ROLE_BAILLEUR (le service filtre par `idUser` si le rôle est BAILLEUR — un bailleur ne voit que ses cours). Query : `idSecteur?`, pagination.
Réponse (data) : `PagedResponse<CourResponse>`.
#### GET /api/cours/{id}
Rôle requis : ROLE_AGENT, ROLE_ADMIN, ROLE_BAILLEUR (filtré par ownership pour bailleur). Réponse : `CourResponse`. Codes : 404.
#### POST /api/cours
Crée une cour. Rôle requis (interface) : ROLE_AGENT, ROLE_ADMIN — **note** : le controller a les `@PreAuthorize` commentés (désactivés) sur create/update/delete ; en l'état actuel du code, seule l'annotation de classe sur l'interface `@PreAuthorize` n'existe pas non plus pour ces méthodes précises — se fier prudemment à `hasAnyRole('AGENT','ADMIN')` documenté en commentaire, mais vérifier en test manuel avant mise en prod frontend.
Body :
```typescript
interface CreateCourRequest {
  idSecteur: number;        // requis
  idProprietaire: number;   // requis — id du bailleur propriétaire
  referenceCour: string;    // requis
  lotCour?: string;
  numeroPorte?: number;
}
```
#### PUT /api/cours/{id}
Body : `{ idSecteur?: number; referenceCour?: string; lotCour?: string; numeroPorte?: number }`.
#### DELETE /api/cours/{id}
Supprime (soft delete).

```typescript
interface CourResponse {
  idCour: number; referenceCour: string; lotCour: string | null; numeroPorte: number | null;
  idSecteur: number; nomSecteur: string; idProprietaire: number; nomProprietaire: string;
}
```

### Maison (`/api/maisons` écriture, `/api/public/maisons` lecture)

#### GET /api/public/maisons
Catalogue public. Rôle : public. Query : `idCour?`, `statut?` (`StatutMaison`), pagination. Réponse (data) : `PagedResponse<MaisonResponse>`.
#### GET /api/public/maisons/{id}
Rôle : public. Réponse : `MaisonResponse`. Codes : 404.
#### POST /api/maisons
Rôle requis : ROLE_AGENT uniquement (le controller restreint à `hasRole('AGENT')`, plus strict que `hasAnyRole('AGENT','ADMIN')` déclaré sur l'interface).
Body :
```typescript
interface CreateMaisonRequest {
  idCour: number;          // requis
  typeMaison?: string;
  nomCommunMaison?: string;
  nombrePiece?: number;
  loyer?: number;
  caution?: number;
  avance?: number;
  nombreMoisCaution?: number;
}
```
#### PUT /api/maisons/{id}
Rôle requis : ROLE_AGENT. Body : mêmes champs que create sauf `idCour`/`avance`, tous optionnels.
#### PATCH /api/maisons/{id}/statut
Rôle requis : ROLE_AGENT. Body : `{ statut: StatutMaison }` (requis). Codes : 409 `INVALID_STATUT_TRANSITION`.
#### DELETE /api/maisons/{id}
Rôle requis : ROLE_ADMIN uniquement.

```typescript
interface MaisonResponse {
  idMaison: number; typeMaison: string | null; nomCommunMaison: string | null; nombrePiece: number | null;
  loyer: number | null; caution: number | null; nombreMoisCaution: number | null;
  statut: StatutMaison; idCour: number; referenceCour: string;
}
```

### Catégorie bien/service (`/api/categories`) — `CategorieBienServiceAPI`

#### GET /api/categories
Lecture ouverte à tout utilisateur authentifié (pas de `@PreAuthorize` sur getAll/getById — nécessite juste d'être connecté, catalogue interne).
Query : pagination. Réponse (data) : `PagedResponse<CategorieResponse>`.
#### GET /api/categories/{id}
Réponse : `CategorieResponse`. Codes : 404.
#### POST /api/categories
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Body : `{ libelle: string; description?: string }` (`libelle` requis).
#### PUT /api/categories/{id}
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Body : `{ libelle?: string; description?: string }`.
#### DELETE /api/categories/{id}
Rôle requis : ROLE_ADMIN uniquement.

```typescript
interface CategorieResponse { idCategorie: number; libelle: string; description: string | null; }
```

### Bien/service (`/api/biens-services` écriture, `/api/public/biens-services` lecture)

#### GET /api/public/biens-services
Catalogue public. Rôle : public. Query : `idSecteur?`, `idCategorie?`, pagination. Réponse (data) : `PagedResponse<BienServiceResponse>`.
#### GET /api/public/biens-services/{id}
Rôle : public. Réponse : `BienServiceResponse`. Codes : 404.
#### POST /api/biens-services
Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Body :
```typescript
interface CreateBienServiceRequest {
  idSecteur: number;      // requis
  idCategorie: number;    // requis
  idGestionnaire: number; // requis
  libelle: string;        // requis
  description?: string;
  prixJournalier?: number;
  prixMensuel?: number;
}
```
#### PUT /api/biens-services/{id}
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Body : mêmes champs, tous optionnels, sans `idGestionnaire`.
#### PATCH /api/biens-services/{id}/disponibilite
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Body : `{ disponibilite: StatutBienService }` (requis).
#### DELETE /api/biens-services/{id}
Rôle requis : ROLE_AGENT, ROLE_ADMIN (pas de restriction supplémentaire observée dans le controller).

```typescript
interface BienServiceResponse {
  idBienService: number; libelle: string; description: string | null;
  prixJournalier: number | null; prixMensuel: number | null; disponibilite: StatutBienService;
  idSecteur: number; nomSecteur: string; idCategorie: number; libelleCategorie: string;
  idGestionnaire: number; nomGestionnaire: string;
}
```

### Location de bien/service (`/api/locations-biens-services`) — `LocationBienServiceAPI`

Workflow en 2 étapes : le client dépose une demande sans paiement (`POST`), puis un agent réceptionne le paiement et confirme (`PATCH .../confirmer`). Peut être prolongée, annulée, remboursée.

#### GET /api/locations-biens-services
Rôle requis : ROLE_AGENT, ROLE_ADMIN (vue globale), ROLE_CLIENT (filtré sur ses propres locations). Query : pagination.
Réponse (data) : `PagedResponse<LocationBienServiceResponse>`.
#### GET /api/locations-biens-services/{id}
Rôle requis : ROLE_AGENT, ROLE_ADMIN, ROLE_CLIENT (ownership). Réponse : `LocationBienServiceResponse`. Codes : 404.
#### POST /api/locations-biens-services
Étape 1 — dépôt de demande. Rôle requis : ROLE_CLIENT uniquement.
Body :
```typescript
interface CreateLocationBienServiceRequest {
  idBienService: number;  // requis
  destination?: string;
  dateDebut: string;      // requis, ISO LocalDateTime
  dateFin: string;        // requis, ISO LocalDateTime
}
```
Codes : 409 `MAISON_INDISPONIBLE`/`CONFLIT_RESERVATION` si applicable au bien.
#### PATCH /api/locations-biens-services/{id}/confirmer
Étape 2 — l'agent encaisse et confirme. Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Body :
```typescript
interface ConfirmerLocationRequest {
  montantPaiement: number; // requis
  modePaiement: string;    // requis
  referencePaiement?: string;
  dateDebut?: string;       // ajustement optionnel négocié au comptoir
  dateFin?: string;
}
```
#### PATCH /api/locations-biens-services/{id}/duree
Prolonge la durée. Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Body :
```typescript
interface ModifierDureeLocationRequest {
  nouvelleDateFin: string;       // requis
  montantComplement?: number;    // si la prolongation génère un paiement
  modePaiementComplement?: string;
  referencePaiementComplement?: string;
}
```
Réponse (data) :
```typescript
interface ModifierDureeLocationResponse {
  location: LocationBienServiceResponse;
  totalEncaisse: number;
  totalRembourse: number;
  solde: number;
}
```
#### PATCH /api/locations-biens-services/{id}/annuler
Rôle requis : ROLE_AGENT, ROLE_ADMIN, ROLE_CLIENT (le client peut annuler sa propre location).
#### POST /api/locations-biens-services/{id}/rembourser
Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Body : `{ montant: number; modeRemboursement: string; reference?: string; motif?: string }` (`montant` requis positif, `modeRemboursement` requis).
Réponse (data) : `RemboursementResponse`.
#### GET /api/locations-biens-services/{id}/remboursements
Rôle requis : ROLE_AGENT, ROLE_ADMIN, ROLE_CLIENT. Réponse (data) : `RemboursementResponse[]`.
#### PATCH /api/locations-biens-services/{id}/statut
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Body : `{ statut: StatutLocationBienService }` (requis). Codes : 409 `INVALID_STATUT_TRANSITION`.

```typescript
interface LocationBienServiceResponse {
  idLocationBienService: number; idClient: number; nomClient: string;
  idBienService: number; libelleBienService: string; destination: string | null;
  dateDebut: string; dateFin: string; duree: number; montantTotal: number;
  statut: StatutLocationBienService;
  historiquePaiements: PaiementLocationBienServiceResponse[];
}
interface PaiementLocationBienServiceResponse {
  idPaiement: number; montant: number; modePaiement: string;
  typePaiement: 'NORMAL' | 'PROLONGATION'; datePaiement: string;
}
interface RemboursementResponse {
  idRemboursement: number; montant: number; modeRemboursement: string;
  reference: string | null; motif: string | null; dateRemboursement: string;
}
```

---

## 7. Contrats

### Contrat mandat (`/api/contrats-mandat`) — `ContratMandatAPI`

Mandat entre bailleur et agence pour gérer une cour. **RG3 : un seul mandat actif par cour à la fois.**
Contrôleur : `@PreAuthorize("hasAnyRole('AGENT','ADMIN','BAILLEUR')")` par défaut, précisé par méthode.

#### GET /api/contrats-mandat
Rôle requis : ROLE_AGENT, ROLE_ADMIN, ROLE_BAILLEUR (filtré par ownership pour bailleur). Query : `idCour?`, `statut?` (`StatutMandat`), pagination.
Réponse (data) : `PagedResponse<ContratMandatResponse>`.
#### GET /api/contrats-mandat/{id}
Rôle requis : ROLE_ADMIN, ROLE_AGENT, ou le bailleur propriétaire de la cour du mandat (`@contratMandatSecurity.isProprietaireCour`). Réponse : `ContratMandatResponse`. Codes : 404, 403.
#### POST /api/contrats-mandat
Rôle requis : ROLE_AGENT uniquement.
Body :
```typescript
interface CreateContratMandatRequest {
  idCour: number;               // requis
  idAgent: number;               // requis
  dateDebut: string;             // requis, ISO LocalDateTime
  dateFin?: string;
  typeMandat: 'GESTION' | 'LOCATION' | 'VENTE'; // requis
  commission?: number;
  modeFacturation?: string;
}
```
Codes : 409 `MANDAT_ACTIF_EXISTANT` (RG3).
#### PATCH /api/contrats-mandat/{id}/activer
Passe le mandat En attente → Actif. Rôle requis : ROLE_AGENT. Codes : 409 `INVALID_STATUT_TRANSITION`, `MANDAT_ACTIF_EXISTANT`.
#### PATCH /api/contrats-mandat/{id}/resilierContratLocation
Résilie le mandat. Rôle requis : ROLE_AGENT.
Body : `{ motifResiliation: string }` (requis).
#### DELETE /api/contrats-mandat/{id}
Rôle requis : ROLE_ADMIN uniquement.

```typescript
interface ContratMandatResponse {
  idMandat: number; idCour: number; referenceCour: string; idAgent: number; nomAgent: string;
  dateDebut: string; dateFin: string | null; typeMandat: 'GESTION' | 'LOCATION' | 'VENTE';
  commission: number | null; statut: StatutMandat; motifResiliation: string | null;
}
```

### Contrat location (`/api/contrats-location`) — `ContratLocationAPI`

Contrat entre locataire et maison. **RG2 : pas de chevauchement de dates pour une même maison.**

#### GET /api/contrats-location
Rôle requis : ROLE_AGENT, ROLE_ADMIN, ROLE_CLIENT, ROLE_BAILLEUR (filtré par ownership). Query : `idMaison?`, `idLocataire?`, pagination.
Réponse (data) : `PagedResponse<ContratLocationResponse>`.
#### GET /api/contrats-location/{id}
Rôle requis : ROLE_AGENT, ROLE_ADMIN, ROLE_CLIENT, ROLE_BAILLEUR (`@PostAuthorize` côté service, ownership vérifié après chargement). Réponse (data) : `ContratLocationResponse`. Codes : 404, 403.
#### POST /api/contrats-location
Crée un contrat de location (généralement issu d'une conversion de réservation). **Réponse SANS enveloppe standard** — retourne directement un `ContratLocationResponseDTO` JSON.
Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Body :
```typescript
interface CreateContratLocationRequest {
  idLocataire: number;      // requis
  idMaison: number;          // requis
  dateEntree: string;        // requis, ISO LocalDateTime
  dateSortie?: string;
  montantLoyer: number;      // requis
  typeContrat?: string;
  etatDesLieuxEntree?: string;
}
```
Codes : 409 `CONFLIT_RESERVATION`/`MAISON_INDISPONIBLE`.
#### PATCH /api/contrats-location/{id}/terminer
Termine normalement le contrat (fin de bail). Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Body : `{ etatDesLieuxSortie: string; dateSortie?: string; fraisReparation: number }` (`etatDesLieuxSortie` et `fraisReparation` requis, `fraisReparation >= 0`). Génère un décompte de sortie.
#### PATCH /api/contrats-location/{id}/resilier
Résiliation anticipée. Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Body : `{ etatDesLieuxSortie: string; dateSortie?: string; coutReparation: number }` (`etatDesLieuxSortie` et `coutReparation` requis, `coutReparation >= 0`).
#### PATCH /api/contrats-location/decomptes-sortie/{idDecompte}/regler
Règle le décompte de sortie (restitution avance/caution ou complément dû). Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Body : `{ modePaiement: string; reference?: string }` (`modePaiement` requis).
#### GET /api/contrats-location/{id}/decompte-sortie
Récupère le décompte de sortie associé au contrat. Rôle requis : ROLE_AGENT, ROLE_ADMIN. Réponse (data) : `DecompteSortieResponse`. Codes : 404.

```typescript
interface ContratLocationResponse {
  idContratLocation: number; idLocataire: number; nomLocataire: string;
  idMaison: number; nomCommunMaison: string; dateEntree: string; dateSortie: string | null;
  montantLoyer: number; statut: StatutLocation;
}
interface DecompteSortieResponse {
  idDecompte: number; idContratLocation: number; dateSortie: string;
  montantAvanceReference: number; montantCautionReference: number; montantArrieres: number;
  coutReparation: number; montantDeduitAvance: number; montantDeduitCaution: number;
  montantManquant: number; montantARembourser: number; statut: StatutDecompteSortie;
}
```

---

## 8. Paiements / échéances

Toute location de bien/service nécessite un paiement préalable (pas de location sans paiement). Les échéances (loyer/commission) sont générées automatiquement chaque mois à partir des contrats mandat/location actifs.

### Paiement (`/api/paiements`) — `PaiementAPI`

Pas de passerelle de paiement en ligne : un paiement n'est enregistré qu'une fois l'argent effectivement reçu par l'agence.

#### POST /api/paiements
Enregistre un paiement couvrant une ou plusieurs échéances. Rôle requis : ROLE_ADMIN, ROLE_AGENT uniquement.
Body :
```typescript
interface CreatePaiementRequest {
  montantPaiement: number;   // requis
  modePaiement: string;      // requis
  referencePaiement?: string;
  datePaiement?: string;      // ISO LocalDateTime, défaut = maintenant côté serveur si absent
  idEcheances: number[];      // requis, au moins un élément
}
```
Codes : 400 `MONTANT_PAIEMENT_INVALIDE`, 409 `ECHEANCE_DEJA_PAYEE`.
#### GET /api/paiements/{id}
Rôle requis : ROLE_ADMIN, ROLE_AGENT, ROLE_CLIENT, ROLE_BAILLEUR (classe), pas de restriction supplémentaire par méthode observée — vérifier ownership côté service. Réponse (data) : `PaiementResponse`. Codes : 404.

```typescript
interface PaiementResponse {
  idPaiement: number; datePaiement: string; montantPaiement: number;
  sens: 'ENTREE' | 'SORTIE'; modePaiement: string; referencePaiement: string | null;
  idEcheancesCouvertes: number[];
}
```

### Échéances (`/api/echeances`) — `EcheanceAPI`

Contrôleur : `@PreAuthorize("hasAnyRole('ADMIN','AGENT','BAILLEUR','CLIENT')")` au niveau classe.

#### GET /api/echeances
Rôle requis : tous rôles authentifiés listés ci-dessus (filtré par ownership pour client/bailleur). Query : `type?` (`TypeEcheance`: `MANDAT`|`LOCATION`), `entiteId?`, `statut?` (`StatutEcheance`), pagination.
Réponse (data) : `PagedResponse<EcheanceResponse>`.
#### GET /api/echeances/{id}
Réponse : `EcheanceResponse`. Codes : 404, 403.
#### GET /api/echeances/en-retard-location
Rôle requis : ROLE_ADMIN, ROLE_AGENT. Réponse (data) : `EcheanceResponse[]`.
#### GET /api/echeances/en-retard-mandat
Rôle requis : ROLE_ADMIN, ROLE_AGENT. Réponse (data) : `EcheanceMandatResponse[]`.
#### POST /api/echeances/mandats/{idMandat}/calculer
Calcule le reversement dû au bailleur pour une période (loyers encaissés − commission). Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Query : `periode` (LocalDate, requis). Réponse (data) : `EcheanceMandatResponse`.
#### PATCH /api/echeances/{idEcheance}/confirmer-virement
Confirme le virement du reversement au bailleur. Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Body : `{ modeVersement: string; reference?: string }` (`modeVersement` requis).
#### GET /api/echeances/mandats/{idMandat}/en-attente
Liste les reversements en attente pour un mandat. Rôle requis : ROLE_AGENT, ROLE_ADMIN. Réponse (data) : `EcheanceMandatResponse[]`.

```typescript
interface EcheanceResponse {
  idEcheance: number; type: 'MANDAT' | 'LOCATION'; entiteId: number;
  dateEcheance: string; moisLibelle: string | null;
  montantDu: number; montantPaye: number; penalite: number | null; statut: StatutEcheance;
}
interface EcheanceMandatResponse {
  idEcheance: number; idMandat: number; periodeMois: string;
  montantLoyersDus: number | null; commissionDeduite: number | null;
  montantNetAReverser: number | null; statut: StatutEcheance;
}
```

---

## 9. Annonces, contacts, demandes, offres

Voir aussi le catalogue public de biens (`/api/public/maisons`, `/api/public/biens-services`).

### Annonces (`/api/annonces` écriture, `/api/public/annonces` lecture)

**RG2 : la date d'expiration d'une annonce doit être postérieure à sa date de publication.**

#### POST /api/annonces
Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Body :
```typescript
interface CreateAnnonceRequest {
  titre: string;             // requis
  description?: string;
  typeAnnonce?: string;
  dateExpiration: string;    // requis, ISO LocalDateTime, doit être future (@Future)
  prix?: number;
  localisation?: string;
}
```
Codes : 400 `DATE_EXPIRATION_INVALIDE`.
#### PUT /api/annonces/{id}
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Body : mêmes champs, tous optionnels.
#### PATCH /api/annonces/{id}/statut
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Body : `{ statut: StatutAnnonce }` (requis).
#### DELETE /api/annonces/{id}
Rôle requis : ROLE_ADMIN uniquement.
#### GET /api/public/annonces
Rôle : public. Query : `statut?` (`StatutAnnonce`), pagination. Réponse (data) : `PagedResponse<AnnonceResponse>`.
#### GET /api/public/annonces/{id}
Rôle : public. Réponse : `AnnonceResponse`. Codes : 404.

```typescript
interface AnnonceResponse {
  idAnnonce: number; titre: string; description: string | null; typeAnnonce: string | null;
  datePublication: string; dateExpiration: string; statut: StatutAnnonce;
  prix: number | null; localisation: string | null;
}
```

### Contacts (`/api/contacts` admin, `/api/public/contacts` public — F20)

#### POST /api/public/contacts
Soumission anonyme du formulaire "nous contacter". Rôle : public.
Body :
```typescript
interface CreateContactRequest {
  nomComplet: string;   // requis
  email: string;         // requis, format email
  telephone?: string;
  sujet?: string;
  message: string;       // requis
}
```
Réponse (data) : `ContactResponse`.
#### GET /api/contacts
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Query : `statut?` (`StatutContact`), pagination. Réponse (data) : `PagedResponse<ContactResponse>`.
#### PATCH /api/contacts/{id}/statut
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Body : `{ statut: StatutContact }` (requis).

```typescript
interface ContactResponse {
  idContact: number; nomComplet: string; email: string; telephone: string | null;
  sujet: string | null; message: string; dateEnvoi: string; statut: StatutContact;
}
```

### Demandes (`/api/demandes` admin, `/api/public/demandes` public — F18, besoin exprimé)

#### POST /api/public/demandes
Rôle : public.
Body :
```typescript
interface CreateDemandeRequest {
  nomComplet: string;   // requis
  email?: string;         // format email si présent
  telephone?: string;
  typeBien?: string;
  localisationSouhaite?: string;
  budgetMax?: number;
  description?: string;
}
```
Réponse (data) : `DemandeResponse`.
#### GET /api/demandes
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Query : `statut?` (`StatutDemande`), pagination. Réponse (data) : `PagedResponse<DemandeResponse>`.
#### GET /api/demandes/{id}
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Réponse : `DemandeResponse`. Codes : 404.
#### PATCH /api/demandes/{id}/statut
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Body : `{ statut: StatutDemande }` (requis).

```typescript
interface DemandeResponse {
  idDemande: number; nomComplet: string; email: string | null; telephone: string | null;
  typeBien: string | null; localisationSouhaite: string | null; budgetMax: number | null;
  description: string | null; dateDemande: string; statut: StatutDemande;
}
```

### Offres (`/api/offres` admin, `/api/public/offres` public — F19, dépôt spontané)

#### POST /api/public/offres
Rôle : public.
Body :
```typescript
interface CreateOffreRequest {
  nomComplet: string; // requis
  email?: string;
  telephone?: string;
  typeOffre?: string;
  titre?: string;
  description?: string;
  adresse?: string;
}
```
Réponse (data) : `OffreResponse`.
#### GET /api/offres
Rôle requis : ROLE_AGENT, ROLE_ADMIN (déclaré sur l'interface au niveau classe — **le controller `OffreAdminController` ne répète pas `@PreAuthorize` par méthode**, contrairement aux autres modules ; se fier à l'annotation de classe de l'interface). Query : `statut?` (`StatutOffre`), pagination. Réponse (data) : `PagedResponse<OffreResponse>`.
#### GET /api/offres/{id}
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Réponse : `OffreResponse`. Codes : 404.
#### PATCH /api/offres/{id}/statut
Rôle requis : ROLE_AGENT, ROLE_ADMIN. Body : `{ statut: StatutOffre }` (requis).

```typescript
interface OffreResponse {
  idOffre: number; nomComplet: string; email: string | null; telephone: string | null;
  typeOffre: string | null; titre: string | null; description: string | null; adresse: string | null;
  dateOffre: string; statut: StatutOffre;
}
```

---

## 10. Réservations (`/api/reservations`) — `ReservationAPI`

Réservation d'une maison, convertible ensuite en contrat de location. **RG2 : pas de chevauchement de dates pour une même maison** (partagé avec `contrat_location`).
Contrôleur : `@PreAuthorize("hasAnyRole('CLIENT','AGENT','ADMIN','BAILLEUR')")` par défaut, précisé par méthode.

### GET /api/reservations
Rôle requis : tous rôles listés ci-dessus (filtré par ownership pour client/bailleur). Query : `idMaison?`, pagination.
Réponse (data) : `PagedResponse<ReservationResponse>`.
### GET /api/reservations/{id}
Rôle requis : ROLE_ADMIN, ROLE_AGENT, ou le propriétaire de la maison (`@reservationSecurity.isProprietaireMaison`), ou l'auteur de la réservation (`@reservationSecurity.isOwner`). Réponse : `ReservationResponse`. Codes : 404, 403.
### POST /api/reservations
Crée une réservation. Rôle requis : ROLE_CLIENT, ROLE_AGENT, ROLE_ADMIN (le bailleur ne réserve pas son propre bien).
Body : `{ idMaison: number; dateDebut: string; dateFin: string }` (`idMaison`, `dateFin` requis ; `dateDebut` requis et `@FutureOrPresent`).
Codes : 409 `CONFLIT_RESERVATION`/`MAISON_INDISPONIBLE`.
### PATCH /api/reservations/{id}/confirmer
Rôle requis : ROLE_ADMIN, ROLE_AGENT uniquement (le bailleur ne tranche pas).
### PATCH /api/reservations/{id}/annuler
Rôle requis : ROLE_ADMIN, ROLE_AGENT, ou l'auteur de la réservation (rétractation) — pas le bailleur.
### PATCH /api/reservations/{id}/convertir
Convertit la réservation en contrat de location. Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Query : `montantLoyer` (number, requis), `typeContrat?` (string).
Réponse (data) : `ConversionResponse`.

```typescript
interface ReservationResponse {
  idReservation: number; idUser: number; nomUser: string; idMaison: number; nomCommunMaison: string;
  dateDebut: string; dateFin: string; statut: StatutReservation;
}
interface ConversionResponse {
  idReservation: number;
  contratLocation: ContratLocationResponse; // voir section Contrats
}
```

---

## 11. Médias (`/api/medias` admin, `/api/public/medias` public)

Formats acceptés : JPEG, PNG, WEBP. Taille max 5 Mo (`APP_MEDIAS_MAX_SIZE_IMAGE_MO`). Une image principale (`isPrincipal`) par entité. Stockage MinIO interne — jamais exposé directement, tout transite par le backend.

### POST /api/medias
Upload d'un fichier (multipart/form-data). Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Body (multipart, pas de JSON) :
```typescript
interface UploadMediaRequest {
  entiteType: TypeEntiteMedia; // 'COUR' | 'ANNONCE' | 'MAISON'
  entiteId: number;
  fichier: File;                // champ multipart
  isPrincipal?: boolean;
}
```
Codes : 400 `FORMAT_MEDIA_INVALIDE`, 413 `TAILLE_MEDIA_EXCESSIVE`/`IMAGE_DIMENSIONS_EXCESSIVE`, 500 `MEDIA_STORAGE_ERROR`.
### POST /api/medias/multiple
Upload multiple (max 10 fichiers). Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Body (multipart) : `entiteType`, `entiteId`, `fichiers: File[]` (1 à 10), `isPrincipal?` (s'applique au premier fichier uniquement).
Réponse (data) : `UploadMultipleResult`.
### DELETE /api/medias/{id}
Rôle requis : ROLE_AGENT, ROLE_ADMIN.
### PATCH /api/medias/{id}/principal
Définit ce média comme image principale de son entité. Rôle requis : ROLE_AGENT, ROLE_ADMIN.
### PATCH /api/medias/reorder
Réordonne les médias d'une entité. Rôle requis : ROLE_AGENT, ROLE_ADMIN.
Body : `{ idsMediaOrdonnes: number[] }` (requis, liste ordonnée des ids dans l'ordre d'affichage souhaité).
### GET /api/public/medias
Liste les médias d'une entité (uniquement `ANNONCE` ou `MAISON` — l'implémentation doit rejeter les autres types). Rôle : public.
Query : `entiteType` (`TypeEntiteMedia`, requis), `entiteId` (number, requis). Réponse (data) : `MediaResponse[]`.
### GET /api/public/medias/{id}/fichier
Sert l'image en taille réelle (binaire, pas JSON). Rôle : public.
### GET /api/public/medias/{id}/miniature
Sert la miniature (binaire). Rôle : public.

```typescript
type TypeEntiteMedia = 'COUR' | 'ANNONCE' | 'MAISON';
interface MediaResponse {
  idMedia: number; entiteType: TypeEntiteMedia; entiteId: number;
  typeMedia: string; url: string; urlThumbnail: string | null;
  statutThumbnail: StatutThumbnail; isPrincipal: boolean; ordre: number; dateUpload: string;
}
interface UploadMultipleResult {
  reussis: MediaResponse[];
  echecs: { nomFichier: string; motif: string }[];
}
```

---

## 12. Documents (génération PDF signés QR code)

Tous les endpoints retournent un flux binaire `application/pdf` (pas de JSON, pas d'enveloppe `ApiResponse`), avec `Content-Disposition: attachment; filename=...`. Les PDF sont signés HMAC-SHA256 (`Utils.signer`, secret `documents.secret` / env `DOCUMENTS_SECRET`) avec un QR code (`com.google.zxing`) intégré en pied de page, mise en page via `openpdf`.

**⚠️ Fonctionnalité de vérification QR incomplète (confirmé par lecture de code, 2026-09-28)** — ne pas prévoir de flux "scanner le QR pour vérifier un document" côté frontend tant que ce point n'est pas traité côté backend :
- Le QR code n'encode **pas une URL** mais du texte brut pipe-delimité (`numero|champ1|champ2|...|signature`, signature = HMAC-SHA256 tronquée à 12 caractères base64url). Le scanner d'un téléphone affichera juste ce texte, sans lien cliquable.
- Le paramètre `urlVerificationBase` de `Utils.ajouterPied(...)` est déclaré mais **jamais utilisé dans le corps de la méthode** (code mort) ; les 8 services documents lui passent soit une constante vide, soit `""` en dur.
- **Aucun endpoint de vérification publique n'existe** (recherche exhaustive sur "verif"/"QR"/"signer(" dans `modules/documents` : seules les classes de génération utilisent `signer()`, aucune ne le vérifie côté serveur).
- Le cahier des charges mentionne cette vérification ; elle est donc à considérer comme une fonctionnalité **non livrée**, pas comme un bug mineur.

Sécurité : contrairement à l'interface/au controller qui ne portent souvent aucun `@PreAuthorize`, **les règles réelles sont posées sur la méthode du *service*** (`*DocumentService`) pour tous les endpoints sauf `recus` (règle sur le controller) et `rapports-mensuels` (règle sur l'interface). Entité introuvable → 404 `ResourceNotFoundException` avec un code `<RESSOURCE>_NOT_FOUND` (ex. `CONTRAT_DE_LOCATION_NOT_FOUND`) ; accès refusé → 403 `ACCESS_DENIED` ; document pas encore disponible (ex. décompte non réglé) → 409 `INVALID_STATE_TRANSITION`.

### GET /api/documents/attestations-loyer/{idContrat}
Génère à la volée (jamais stockée) une attestation de loyer reflétant la situation de paiement actuelle (arriérés calculés en direct).
Rôle : `hasAnyRole('ADMIN','AGENT') or @contratLocationSecurity.isAccessible(#idContrat, principal.idUser)` (règle sur `AttestationLoyerDocumentService`).
Réponse : PDF (`attestation-loyer.pdf`). Codes : 404 `CONTRAT_DE_LOCATION_NOT_FOUND`, 403 `ACCESS_DENIED`.

### GET /api/documents/contrats-location/{idContrat}
Génère une seule fois (document juridique immuable, stocké) ou récupère le contrat de location PDF signé.
Rôle : `hasAnyRole('ADMIN','AGENT') or @contratLocationSecurity.isAccessible(#idContrat, principal.idUser)` (sur `ContratLocationDocumentService`).
Réponse : PDF (`contrat-location.pdf`). Codes : 404 `CONTRAT_DE_LOCATION_NOT_FOUND`, 403 `ACCESS_DENIED`.

### GET /api/documents/contrats-mandat/{idMandat}
Génère une seule fois ou récupère le contrat de mandat PDF signé (bailleur ↔ agent).
Rôle : `hasAnyRole('ADMIN','AGENT') or @contratMandatSecurity.isAccessible(#idMandat, principal.idUser)` (sur `ContratMandatDocumentService`).
Réponse : PDF (`contrat-mandat.pdf`). Codes : 404 `CONTRAT_DE_MANDAT_NOT_FOUND`, 403 `ACCESS_DENIED`.

### GET /api/documents/decomptes-sortie/{idDecompte}
Génère ou récupère le décompte de sortie PDF — disponible uniquement une fois le décompte au statut `REGLE`.
Rôle : `hasAnyRole('ADMIN','AGENT') or @decompteSortieSecurity.isAccessible(#idDecompte, principal.idUser)` (sur `DecompteSortieDocumentService`).
Réponse : PDF (`decompte-sortie.pdf`). Codes : 404 `DÉCOMPTE_DE_SORTIE_NOT_FOUND` ; 409 `INVALID_STATE_TRANSITION` si `statut != REGLE` ; 403 `ACCESS_DENIED`.

### GET /api/documents/etats-des-lieux/{idContrat}/entree
État des lieux d'entrée, généré à partir du champ texte `ContratLocation.etatDesLieuxEntree`.
Rôle : `hasAnyRole('ADMIN','AGENT') or @contratLocationSecurity.isAccessible(#idContrat, principal.idUser)` (sur `EtatDesLieuxDocumentService.genererEntree`).
Réponse : PDF (`etat-des-lieux-entree.pdf`). Codes : 404 `CONTRAT_DE_LOCATION_NOT_FOUND` ; 409 `INVALID_STATE_TRANSITION` si le champ est vide ; 403 `ACCESS_DENIED`.

### GET /api/documents/etats-des-lieux/{idContrat}/sortie
État des lieux de sortie, disponible uniquement après clôture du contrat (`TERMINE`/`RESILIE`).
Rôle : identique à l'endpoint entrée (sur `EtatDesLieuxDocumentService.genererSortie`).
Réponse : PDF (`etat-des-lieux-sortie.pdf`). Codes : 404 `CONTRAT_DE_LOCATION_NOT_FOUND` ; 409 `INVALID_STATE_TRANSITION` si le contrat n'est pas clôturé ou si le champ est vide ; 403 `ACCESS_DENIED`.

### GET /api/documents/quittances/{idEcheance}
Quittance de loyer — disponible uniquement une fois l'échéance intégralement payée.
Rôle : `hasAnyRole('ADMIN','AGENT') or @quittanceLoyerSecurity.isAccessible(#idEcheance, principal.idUser)` (sur `QuittanceLoyerDocumentService`).
Réponse : PDF (`quittance-loyer.pdf`). Codes : 404 `ÉCHÉANCE_NOT_FOUND`/`CONTRAT_DE_LOCATION_NOT_FOUND` ; 409 `INVALID_STATE_TRANSITION` si le type d'échéance n'est pas `LOCATION` ou si `statut != PAYE` ; 403 `ACCESS_DENIED`.

### GET /api/documents/rapports-mensuels?periode={date}
Rapport mensuel de gestion (loyers/commissions encaissés, retards, locations biens/services, décomptes réglés) pour le mois de `periode`. Un seul rapport par mois, immuable une fois généré ; un job planifié en génère un automatiquement chaque 15 du mois, mais il reste générable à la demande.
Rôle requis : `ROLE_ADMIN` uniquement — `@PreAuthorize("hasRole('ADMIN')")` posé directement sur l'interface `RapportMensuelDocumentAPI`.
Query : `periode` (LocalDate, requis ; seul le mois/année comptent).
Réponse : PDF (`rapport-mensuel.pdf`). Codes : aucune exception métier spécifique (un rapport est toujours calculable, même à zéro).

### GET /api/documents/recus/{idPaiement}
Génère une seule fois (immuable, stocké) ou récupère le reçu de paiement PDF.
Rôle requis : `@PreAuthorize` posé sur le **controller** `RecuDocumentController.genererOuRecuperer` : `hasAnyRole('AGENT','ADMIN') or @paiementOwnershipResolver.isPaiementAccessible(#idPaiement, principal.idUser, false) or @paiementOwnershipResolver.isPaiementAccessible(#idPaiement, principal.idUser, true) or @paiementOwnershipResolver.isPaiementAccessibleBienService(#idPaiement, principal.idUser)`.
Réponse : PDF (`recu.pdf`). Codes : 404 `PAIEMENT_NOT_FOUND`, 403 `ACCESS_DENIED`.

---

## 13. Journal d'audit (`/api/journaux`) — `JournalAPI`

Rôle requis : ROLE_ADMIN uniquement (`@PreAuthorize("hasRole('ADMIN')")` au niveau classe du controller).

### GET /api/journaux
Consulte la piste d'audit (qui a fait quoi, quand, avant/après).
Query : `idUser?`, `action?`, `entite?`, `dateDebut?` (date ISO), `dateFin?` (date ISO), pagination.
Réponse (data) : `PagedResponse<JournalResponse>`.

```typescript
interface JournalResponse {
  idJournal: number; idUser: number | null; action: string; entite: string;
  ligneEntite: number | null; description: string | null; dateAction: string;
  ancienContenu: string | null; nouveauContenu: string | null;
}
```

---

## 14. Paramètres système (`/api/parametres`) — `ParametreAPI`

Rôle requis : ROLE_ADMIN uniquement (les deux endpoints ont `@PreAuthorize("hasRole('ADMIN')")` explicite sur l'interface).

### GET /api/parametres
Liste tous les paramètres système. Réponse (data) : `ParametreResponse[]`.
### PATCH /api/parametres/{cle}
Met à jour la valeur d'un paramètre par sa clé.
Body : `{ valeur: string }` (requis).

```typescript
type TypeValeurParametre = 'ENTIER' | 'DECIMAL' | 'BOOLEEN' | 'TEXTE';
interface ParametreResponse {
  idParametre: number; cle: string; valeur: string;
  typeValeur: TypeValeurParametre; description: string | null;
}
// Clés connues (donnees/parametres/model/CleParametre.java) :
// TOLERANCE_LOCATION_JOURS, TOLERANCE_MANDAT_JOURS, MEDIAS_RETENTION_JOURS,
// MEDIAS_MAX_FICHIERS_PAR_LOT, PENALITE_RETARD_MONTANT
```

---

## 15. Statistiques

### Public (`/api/stats/public`) — seule section stats accessible sans compte

#### GET /api/stats/public
Rôle : public. Réponse (data) :
```typescript
interface PublicStatsResponse {
  maisonsDisponibles: number; biensDisponibles: number; villesCouvertes: number;
  secteursCouverts: number; proprietesGerees: number; locationsRealisees: number;
}
```
#### GET /api/stats/public/annonces
Les 5 dernières annonces actives publiées. Rôle : public.
**Attention** : le service sérialise directement l'entité JPA `Annonce` (pas de DTO dédié) — tous les champs mappés/visibles par Jackson sur l'entité sont exposés, pas seulement ceux d'`AnnonceResponse`.

### Admin (`/api/stats/admin/**`) — `StatsAPI`, ROLE_ADMIN uniquement

#### GET /api/stats/admin
Tableau de bord global. Réponse (data) : `AdminStatsResponse` (voir ci-dessous).
#### GET /api/stats/admin/villes
Répartition maisons/locataires par ville. Réponse (data) : `VilleStatsDTO[]`.
#### GET /api/stats/admin/bailleurs/top
Top bailleurs par nombre de maisons. Query : `limite?` (number, défaut 10). Réponse (data) : `BailleurTopDTO[]`.
#### GET /api/stats/admin/locataires/creances
Locataires en retard de paiement. Réponse (data) : `LocataireCreanceDTO[]`.
#### GET /api/stats/admin/bailleurs/creanciers
Bailleurs à qui l'agence doit encore reverser des fonds. Réponse (data) : `BailleurCreancierDTO[]`.
#### GET /api/stats/admin/gain-mensuel
Gain de l'agence (commissions) pour une période. Query : `periode?` (LocalDate). Réponse (data) : `GainAgenceDTO`.

```typescript
interface AdminStatsResponse {
  totalUtilisateurs: number; utilisateursParRole: Record<string, number>;
  inscriptionsCeMois: number; connexionsRecentes7j: number;
  totalCours: number; maisonsParStatut: Record<string, number>; tauxOccupationMaisons: number;
  biensServiceParDisponibilite: Record<string, Record<string, number>>;
  mandatsParStatut: Record<string, number>; contratsLocationParStatut: Record<string, number>;
  mandatsExpirantSous30Jours: number;
  montantDuTotal: number; montantPayeTotal: number;
  nombreEcheancesEnRetard: number; montantEcheancesEnRetard: number;
  locationsBienServiceParStatut: Record<string, number>;
  totalEncaisseBienService: number; totalRembourseBienService: number;
  annoncesParStatut: Record<string, number>; demandesParStatut: Record<string, number>;
  offresNonTraitees: number; contactsNonLus: number;
}
interface VilleStatsDTO { nomVille: string; nbMaisons: number; nbLocatairesActifs: number; }
interface BailleurTopDTO { idBailleur: number; nomComplet: string; nbMaisons: number; }
interface LocataireCreanceDTO { idLocataire: number; nomComplet: string; nbEcheancesEnRetard: number; montantDu: number; }
interface BailleurCreancierDTO { idBailleur: number; nomComplet: string; periodeMois: string; montantDu: number; }
interface GainAgenceDTO { periodeMois: string; totalCommissions: number; totalReverseAuxBailleurs: number; }
```

### Agent (`/api/stats/agent/**`) — ROLE_AGENT, ROLE_ADMIN

#### GET /api/stats/agent/demandes-en-attente
Réponse (data) : `DemandeEnAttenteDTO[]`.
#### GET /api/stats/agent/reservations-en-attente
Réponse (data) : `ReservationEnAttenteDTO[]`.
#### GET /api/stats/agent/mes-mandats
Mandats gérés par l'agent connecté. Réponse (data) : `BailleurMandatDTO[]`.
#### GET /api/stats/agent/echeances-retard
Réponse (data) : `EcheanceRetardAgentDTO[]`.

```typescript
interface DemandeEnAttenteDTO {
  idLocation: number; libelleBien: string; nomClient: string;
  dateDebut: string; dateFin: string; montantEstime: number; statut: StatutLocationBienService;
}
interface ReservationEnAttenteDTO { idReservation: number; nomMaison: string; nomClient: string; dateDebut: string; dateFin: string; }
interface BailleurMandatDTO {
  idMandat: number; referenceCour: string; statut: string; dateDebut: string; dateFin: string | null;
  commissionPourcentage: number | null; nomAgent: string;
}
interface EcheanceRetardAgentDTO { idEcheance: number; type: 'LOCATION' | 'MANDAT'; libelleContrat: string; montantDu: number; dateEcheance: string; }
```

### Bailleur (`/api/stats/bailleur/**`) — ROLE_BAILLEUR, ROLE_ADMIN

#### GET /api/stats/bailleur/patrimoine
Réponse (data) : `BailleurPatrimoineDTO`.
#### GET /api/stats/bailleur/revenus
Réponse (data) : `BailleurRevenuDTO[]`.
#### GET /api/stats/bailleur/mandat
Mandat actif du bailleur connecté. Réponse (data) : `BailleurMandatDTO`. Codes : 404 `MANDAT_ACTIF_NOT_FOUND` si aucun mandat actif.

```typescript
interface BailleurPatrimoineDTO { nbCours: number; nbMaisons: number; maisonsParStatut: Record<string, number>; tauxOccupation: number | null; }
interface BailleurRevenuDTO { periodeMois: string; montantDu: number | null; montantRecu: number | null; statut: 'EN_ATTENTE' | 'PAYE' | string; }
```

### Client (`/api/stats/client/**`) — ROLE_CLIENT uniquement

#### GET /api/stats/client/locations
Réponse (data) : `ClientLocationDTO[]`.
#### GET /api/stats/client/echeances
Réponse (data) : `ClientEcheanceDTO[]`.
#### GET /api/stats/client/remboursements
Réponse (data) : `ClientRemboursementDTO[]`.

```typescript
interface ClientLocationDTO {
  idLocation: number; libelleBien: string; dateDebut: string; dateFin: string;
  montantTotal: number; totalEncaisse: number; solde: number; statut: StatutLocationBienService;
}
interface ClientEcheanceDTO { idEcheance: number; moisLibelle: string | null; dateEcheance: string; montantDu: number; montantPaye: number; statut: StatutEcheance; }
interface ClientRemboursementDTO { idRemboursement: number; montant: number; motif: string | null; dateRemboursement: string; }
```

---

## 16. Types TypeScript partagés

### Enums de statut (valeurs exactes lues dans `donnees/*/model/Statut*.java`)

```typescript
type StatutAnnonce = 'ACTIVE' | 'EXPIREE' | 'SUSPENDUE';
type StatutContact = 'NON_LU' | 'LU' | 'TRAITE';
type StatutDemande = 'EN_ATTENTE' | 'EN_COURS' | 'SATISFAITE' | 'ANNULEE';
type StatutOffre = 'ACTIVE' | 'EXPIREE' | 'SUSPENDUE';

type StatutBienService = 'DISPONIBLE' | 'RESERVEE' | 'LOUEE' | 'EN_MAINTENANCE';
type StatutLocationBienService = 'EN_ATTENTE' | 'ACTIF' | 'TERMINE' | 'ANNULE';
type StatutMaison = 'DISPONIBLE' | 'RESERVEE' | 'LOUEE' | 'EN_MAINTENANCE';

type StatutDecompteSortie = 'EN_ATTENTE' | 'REGLE';
type StatutLocation = 'EN_ATTENTE' | 'ACTIF' | 'TERMINE' | 'RESILIE';
type StatutMandat = 'EN_ATTENTE' | 'ACTIF' | 'RESILIE' | 'EXPIRE';
type TypeMandat = 'GESTION' | 'LOCATION' | 'VENTE';

type StatutThumbnail = 'EN_COURS' | 'PRET' | 'ECHEC';

type StatutEcheance = 'EN_ATTENTE' | 'PAYE' | 'EN_RETARD' | 'ANNULE';
type TypeEcheance = 'MANDAT' | 'LOCATION';
type SensPaiement = 'ENTREE' | 'SORTIE';
type TypePaiementLocationBienService = 'NORMAL' | 'PROLONGATION';
type TypeEntiteRemboursement = 'LOCATION_BIEN_SERVICE' | 'CONTRA_LOCATION' | 'ECHEANCE_LOYER'; // sic, "CONTRA_LOCATION" dans le code source

type StatutReservation = 'EN_ATTENTE' | 'CONFIRMEE' | 'ANNULEE' | 'CONVERTIE';

type TypeEntiteMedia = 'COUR' | 'ANNONCE' | 'MAISON';

type TypeValeurParametre = 'ENTIER' | 'DECIMAL' | 'BOOLEEN' | 'TEXTE';

type ERole =
  | 'ROLE_CLIENT' | 'ROLE_BAILLEUR' | 'ROLE_SECRETAIRE' | 'ROLE_AGENT'
  | 'ROLE_SG' | 'ROLE_DIRECTEUR' | 'ROLE_DG' | 'ROLE_PDG' | 'ROLE_ADMIN';
```

### Enveloppes génériques

```typescript
interface ApiResponse<T = unknown> {
  success: true;
  message: string;
  code: string;
  timestamp: string;
  data?: T;
}

interface ApiError {
  success: false;
  message: string;
  code: string;
  timestamp: string;
  debug_message?: string;
  exception_type?: string;
}

interface ApiValidationError {
  success: false;
  message: string;
  code: 'VALIDATION_ERROR';
  timestamp: string;
  errors: Record<string, string>;
}

interface PagedResponse<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
```

---

## 17. Zones d'incertitude (non confirmées par simple lecture statique)

- ~~`POST /api/auth/signup` : forme exacte de `data`~~ — non résolu, reste à tester manuellement (voir plus bas).
- ✅ **Résolu (2026-09-28)** — `PaysAdminAPI`/`VilleAdminAPI`/`SecteurAdminAPI` : confirmé par test réel (agent connecté) que le `@PreAuthorize` du *controller* l'emporte bien sur celui de l'interface (Spring résout la méthode la plus spécifique). Les 3 interfaces ont été corrigées pour porter les annotations method-level exactes (`create`/`update` = `hasAnyRole('ADMIN','AGENT')`, `delete` = `hasRole('ADMIN')`), le contrat documenté correspond maintenant au comportement réel.
- ✅ **Résolu (2026-09-28)** — `CourController` : les `@PreAuthorize` de `create`/`update`/`delete` étaient bien désactivés (aucune protection effective au-delà de l'authentification simple). Confirmé par test réel : un `ROLE_CLIENT` pouvait créer/modifier/supprimer une cour avant le fix. Corrigé : les 5 méthodes du controller portent maintenant les mêmes annotations que `CourAPI`.
- ✅ **Résolu (2026-09-28)** — Vérification QR des documents : confirmé absente (voir section 12) et documentée comme fonctionnalité non livrée, pas de fix appliqué (décision produit à prendre séparément).
- ✅ **Résolu et éclairci (2026-09-28)** — `OffreAdminAPI`/`OffreAdminController` : testé en réel (`ROLE_CLIENT` → `GET /api/offres` → `403` **avant tout fix**). **Découverte importante** : quand le controller ne porte aucune annotation `@PreAuthorize` (ni active ni commentée), Spring applique bien celle de l'interface (repli/fallback confirmé empiriquement). Cela signifie que le point "CourController" ci-dessus était probablement déjà protégé par `CourAPI` avant le fix (annotations *commentées*, donc absentes du bytecode — situation identique à Offre) : le code était trompeur à la lecture, mais l'exploitation réelle était vraisemblablement déjà bloquée par ce mécanisme de repli. Les deux fixes (Cour, Offre) restent appliqués par cohérence de convention (le controller répète toujours l'annotation ailleurs dans le code) et par prudence (le comportement de repli de Spring n'est pas garanti contractuellement et pourrait changer). **Conséquence pour l'audit des autres modules** : ne plus considérer "annotation absente sur le controller" comme automatiquement une faille — vérifier d'abord si l'interface porte l'annotation avant de conclure à un problème.
- `GET /api/documents/**` (hors rapport mensuel et reçu) : aucune règle de rôle/ownership explicite trouvée au niveau controller/interface ; l'accès semble contrôlé plus bas (dans les `*DocumentService`, non audités ligne à ligne) — à valider par test manuel pour chaque type de document avant intégration frontend, notamment pour s'assurer qu'un client ne peut pas récupérer le contrat d'un autre locataire.
- `GET /api/paiements/{id}` : pas de restriction par méthode au-delà de l'annotation de classe (`hasAnyRole('ADMIN','AGENT','CLIENT','BAILLEUR')`) — ownership potentiellement non vérifié à ce niveau précis (à comparer avec `PaiementOwnershipResolver` utilisé ailleurs pour les reçus).
- `POST /api/auth/signup` renvoie `throws Exception` sur l'interface — le mapping d'erreur exact pour un échec inattendu à ce endpoint spécifique (hors validations/409 déjà documentées) n'a pas été vérifié par test.
- Champs `sexe` dans les DTOs utilisateur : typé `Character` côté Java (donc un seul caractère `M`/`F`), mappé ici en `string` TypeScript par simplicité — à contraindre à `'M' | 'F'` côté frontend si validation stricte souhaitée.
