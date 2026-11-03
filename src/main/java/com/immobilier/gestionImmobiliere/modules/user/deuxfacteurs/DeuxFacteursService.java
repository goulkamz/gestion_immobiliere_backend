package com.immobilier.gestionImmobiliere.modules.user.deuxfacteurs;

import com.immobilier.gestionImmobiliere.donnees.user.model.CodeSecours2fa;
import com.immobilier.gestionImmobiliere.donnees.user.model.ERole;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.CodeSecours2faRepository;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.exceptions.DeuxFacteursException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.user.jwt.JwtUtils;
import com.immobilier.gestionImmobiliere.modules.user.jwt.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.WebUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Double authentification (TOTP) facultative pour le personnel. Le client et le bailleur
 * n'y ont pas acces. Etat temporaire (defi de connexion, appareils de confiance, codes deja
 * utilises) dans Redis ; secret et codes de secours en base.
 */
@Service
public class DeuxFacteursService {

    public static final String EMETTEUR = "GestImmo";
    public static final String COOKIE_APPAREIL = "appareil_confiance";

    private static final Set<ERole> ROLES_SANS_2FA = Set.of(ERole.ROLE_CLIENT, ERole.ROLE_BAILLEUR);
    private static final Duration DUREE_DEFI = Duration.ofMinutes(5);
    private static final Duration FENETRE_ECHECS = Duration.ofMinutes(15);
    private static final int MAX_ECHECS = 5;
    private static final int NB_CODES_SECOURS = 10;
    private static final String ALPHABET_SECOURS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UserRepository userRepository;
    private final CodeSecours2faRepository codeSecoursRepository;
    private final StringRedisTemplate redis;
    private final TotpService totpService;
    private final ChiffrementSecretService chiffrement;
    private final PasswordEncoder encoder;
    private final RateLimitService rateLimitService;
    private final JwtUtils jwtUtils;
    private final Duration dureeConfiance;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final SecureRandom random = new SecureRandom();

    public DeuxFacteursService(UserRepository userRepository,
                               CodeSecours2faRepository codeSecoursRepository,
                               StringRedisTemplate redis,
                               TotpService totpService,
                               ChiffrementSecretService chiffrement,
                               PasswordEncoder encoder,
                               RateLimitService rateLimitService,
                               JwtUtils jwtUtils,
                               @Value("${app.security.twofa-trust-hours:12}") long heuresConfiance,
                               @Value("${jwt.cookie.secure:true}") boolean cookieSecure,
                               @Value("${jwt.cookie.same-site:Strict}") String cookieSameSite) {
        this.userRepository = userRepository;
        this.codeSecoursRepository = codeSecoursRepository;
        this.redis = redis;
        this.totpService = totpService;
        this.chiffrement = chiffrement;
        this.encoder = encoder;
        this.rateLimitService = rateLimitService;
        this.jwtUtils = jwtUtils;
        this.dureeConfiance = Duration.ofHours(heuresConfiance);
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    // ===================== Gestion du compte =====================

    public record Statut(boolean disponible, boolean actif, long codesSecoursRestants, long heuresConfiance) {}

    public record Initialisation(String secret, String urlOtpAuth, String qrCode) {}

    @Transactional(readOnly = true)
    public Statut statut(Integer idUser) {
        User user = charger(idUser);
        long restants = user.isTotpActif() ? codeSecoursRepository.countByIdUserAndUtiliseLeIsNull(idUser) : 0;
        return new Statut(estDisponiblePour(user), user.isTotpActif(), restants, dureeConfiance.toHours());
    }

    @Transactional
    public Initialisation initier(Integer idUser) {
        User user = charger(idUser);
        exigerDisponible(user);
        if (user.isTotpActif()) {
            throw new DeuxFacteursException(HttpStatus.CONFLICT, "TWO_FACTOR_ALREADY_ACTIVE",
                    "La double authentification est déjà activée.");
        }
        String secret = totpService.genererSecret();
        user.setTotpSecret(chiffrement.chiffrer(secret));
        userRepository.save(user);
        String url = totpService.urlOtpAuth(EMETTEUR, user.getEmail(), secret);
        return new Initialisation(secret, url, totpService.qrCodeDataUri(url));
    }

    /** Valide le premier code, active la fonction et retourne les codes de secours (affichés une seule fois). */
    @Transactional
    public List<String> confirmer(Integer idUser, String code) {
        User user = charger(idUser);
        exigerDisponible(user);
        if (user.isTotpActif() || user.getTotpSecret() == null) {
            throw new DeuxFacteursException(HttpStatus.CONFLICT, "TWO_FACTOR_NO_PENDING_SETUP",
                    "Aucune activation en cours. Recommencez l'activation.");
        }
        exigerCodeValide(user, code, false);
        user.setTotpActif(true);
        userRepository.save(user);
        return genererCodesSecours(idUser);
    }

    @Transactional
    public void desactiver(Integer idUser, String motDePasse, String code) {
        User user = charger(idUser);
        if (!user.isTotpActif()) {
            throw new DeuxFacteursException(HttpStatus.CONFLICT, "TWO_FACTOR_NOT_ACTIVE",
                    "La double authentification n'est pas activée.");
        }
        if (motDePasse == null || !encoder.matches(motDePasse, user.getPassword())) {
            throw new DeuxFacteursException(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "Mot de passe incorrect.");
        }
        exigerCodeValide(user, code, true);
        effacer(user);
    }

    @Transactional
    public List<String> regenererCodesSecours(Integer idUser, String code) {
        User user = charger(idUser);
        if (!user.isTotpActif()) {
            throw new DeuxFacteursException(HttpStatus.CONFLICT, "TWO_FACTOR_NOT_ACTIVE",
                    "La double authentification n'est pas activée.");
        }
        exigerCodeValide(user, code, false);
        return genererCodesSecours(idUser);
    }

    /**
     * Réinitialisation par un administrateur (téléphone perdu) : l'utilisateur réactivera s'il le souhaite.
     * Ses sessions ouvertes sont révoquées : il devra se reconnecter.
     */
    @Transactional
    public void reinitialiserParAdmin(Integer idCible) {
        User user = charger(idCible);
        effacer(user);
        jwtUtils.revokeAllUserTokens(user.getEmail());
    }

    // ===================== Connexion =====================

    /** Vrai si le mot de passe seul ne suffit pas : 2FA active et appareil non reconnu. */
    @Transactional(readOnly = true)
    public boolean exigePourConnexion(Integer idUser, HttpServletRequest request) {
        User user = charger(idUser);
        return user.isTotpActif() && !appareilDeConfiance(request, idUser);
    }

    public String creerDefi(Integer idUser) {
        String jeton = jetonAleatoire();
        redis.opsForValue().set(cleDefi(jeton), String.valueOf(idUser), DUREE_DEFI);
        return jeton;
    }

    public Integer idUserDuDefi(String jeton) {
        String valeur = jeton == null ? null : redis.opsForValue().get(cleDefi(jeton));
        if (valeur == null) {
            throw new DeuxFacteursException(HttpStatus.UNAUTHORIZED, "TWO_FACTOR_CHALLENGE_EXPIRED",
                    "La vérification a expiré. Reconnectez-vous.");
        }
        return Integer.valueOf(valeur);
    }

    public void supprimerDefi(String jeton) {
        redis.delete(cleDefi(jeton));
    }

    /** Verifie le code de la connexion (TOTP ou code de secours) ; les echecs sont plafonnes. */
    @Transactional
    public void verifierPourConnexion(Integer idUser, String code) {
        exigerCodeValide(charger(idUser), code, true);
    }

    public void marquerAppareilDeConfiance(Integer idUser, HttpServletResponse response) {
        String jeton = jetonAleatoire();
        redis.opsForValue().set(cleConfiance(idUser, jeton), "1", dureeConfiance);
        ResponseCookie cookie = ResponseCookie.from(COOKIE_APPAREIL, jeton)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(dureeConfiance)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private boolean appareilDeConfiance(HttpServletRequest request, Integer idUser) {
        jakarta.servlet.http.Cookie cookie = WebUtils.getCookie(request, COOKIE_APPAREIL);
        return cookie != null && Boolean.TRUE.equals(redis.hasKey(cleConfiance(idUser, cookie.getValue())));
    }

    // ===================== Internes =====================

    private void exigerCodeValide(User user, String code, boolean accepteSecours) {
        Integer idUser = user.getIdUser();
        String cleEchecs = "2fa-fail:" + idUser;
        if (rateLimitService.estBloque(cleEchecs, MAX_ECHECS)) {
            throw new DeuxFacteursException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS",
                    "Trop de codes erronés. Veuillez réessayer dans quelques minutes.");
        }
        String normalise = code == null ? "" : code.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        boolean valide = normalise.matches("\\d{6}")
                ? totpValide(user, normalise)
                : accepteSecours && secoursValide(idUser, normalise);
        if (!valide) {
            rateLimitService.enregistrerEchec(cleEchecs, FENETRE_ECHECS);
            throw new DeuxFacteursException(HttpStatus.UNAUTHORIZED, "INVALID_TWO_FACTOR_CODE",
                    "Code de vérification incorrect.");
        }
        rateLimitService.reinitialiser(cleEchecs);
    }

    private boolean totpValide(User user, String code) {
        if (user.getTotpSecret() == null) {
            return false;
        }
        long periode = totpService.verifier(chiffrement.dechiffrer(user.getTotpSecret()), code);
        if (periode < 0) {
            return false;
        }
        // Un code déjà accepté ne sert qu'une fois (rejeu impossible dans sa fenêtre de validité)
        return Boolean.TRUE.equals(redis.opsForValue()
                .setIfAbsent("2fa-used:" + user.getIdUser() + ":" + periode, "1", Duration.ofSeconds(120)));
    }

    private boolean secoursValide(Integer idUser, String codeNormalise) {
        if (codeNormalise.length() != 10) {
            return false;
        }
        return codeSecoursRepository.consommer(idUser, hacher(codeNormalise), LocalDateTime.now()) > 0;
    }

    private List<String> genererCodesSecours(Integer idUser) {
        codeSecoursRepository.deleteByIdUser(idUser);
        codeSecoursRepository.flush();
        List<String> affiches = new ArrayList<>();
        for (int i = 0; i < NB_CODES_SECOURS; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 10; j++) {
                sb.append(ALPHABET_SECOURS.charAt(random.nextInt(ALPHABET_SECOURS.length())));
            }
            String brut = sb.toString();
            codeSecoursRepository.save(new CodeSecours2fa(idUser, hacher(brut)));
            affiches.add(brut.substring(0, 5) + "-" + brut.substring(5));
        }
        return affiches;
    }

    private void effacer(User user) {
        user.setTotpActif(false);
        user.setTotpSecret(null);
        userRepository.save(user);
        codeSecoursRepository.deleteByIdUser(user.getIdUser());
        Set<String> confiances = redis.keys("2fa-trust:" + user.getIdUser() + ":*");
        if (confiances != null && !confiances.isEmpty()) {
            redis.delete(confiances);
        }
        rateLimitService.reinitialiser("2fa-fail:" + user.getIdUser());
    }

    private User charger(Integer idUser) {
        return userRepository.findById(idUser)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", idUser));
    }

    private boolean estDisponiblePour(User user) {
        return user.getRole() != null && !ROLES_SANS_2FA.contains(user.getRole().getLibelleRole());
    }

    private void exigerDisponible(User user) {
        if (!estDisponiblePour(user)) {
            throw new DeuxFacteursException(HttpStatus.FORBIDDEN, "TWO_FACTOR_NOT_AVAILABLE",
                    "La double authentification est réservée au personnel.");
        }
    }

    private String jetonAleatoire() {
        byte[] octets = new byte[32];
        random.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    private static String cleDefi(String jeton) {
        return "2fa-defi:" + hacher(jeton);
    }

    private static String cleConfiance(Integer idUser, String jeton) {
        return "2fa-trust:" + idUser + ":" + hacher(jeton);
    }

    private static String hacher(String valeur) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valeur.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
