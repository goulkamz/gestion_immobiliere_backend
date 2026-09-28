package com.immobilier.gestionImmobiliere.modules.user.services;

import com.immobilier.gestionImmobiliere.donnees.user.model.PasswordResetToken;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.PasswordResetTokenRepository;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.modules.user.jwt.RateLimitService;
import com.immobilier.gestionImmobiliere.utils.ClientIpResolver;
import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

import com.immobilier.gestionImmobiliere.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.MINUTES;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int TOKEN_EXPIRATION_MINUTES = 10;
    private static final int MAX_RESEND_ATTEMPTS = 3;
    private static final int RESEND_COOLDOWN_MINUTES = 5;


    @Transactional
    public ResponseEntity<?> forgotPassword(String email) {

        // Meme reponse que le compte existe ou non : ne pas permettre de tester des emails
        User user = userRepository.findByEmail(email).filter(User::isActive).orElse(null);
        if (user == null) {
            return reponseNeutre();
        }

        // Supprimer les anciens tokens
        tokenRepository.findByEmail(email).ifPresent(token -> {
            if (token.getAttemptCount() >= MAX_RESEND_ATTEMPTS) {
                throw new RuntimeException("Trop de tentatives. Réessayez dans 30 minutes.");
            }
            tokenRepository.deleteByEmail(email);
        });

        // Générer un nouveau token
        String resetToken = generateResetToken();

        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .token(resetToken)
                .creation(Instant.now())
                .expiration(Instant.now().plus(TOKEN_EXPIRATION_MINUTES, MINUTES))
                .used(false)
                .attemptCount(1)
                .build();

        tokenRepository.save(token);

        notificationService.envoyerCodeReinitialisation(email, user.getNom(), resetToken);
        return reponseNeutre();
    }

    private static ResponseEntity<?> reponseNeutre() {
        return buildSuccessResponse(HttpStatus.ACCEPTED, "Si un compte existe avec cet email, vous recevrez un lien de réinitialisation.", "RESET_EMAIL_SENT", null);
    }

    @Transactional
    public ResponseEntity<?> resetPassword(String token, String newPassword, HttpServletRequest request) {

        // Un token se devine par force brute (1 million de combinaisons) : on ne peut pas
        // compter les échecs sur un token précis (une tentative erronée ne correspond à
        // aucune ligne existante), donc on limite le débit de l'endpoint par IP.
        if (rateLimitService.estLimiteDepassee("reset-password:" + extraireIp(request))) {
            throw new TooManyRequestsException("Trop de tentatives. Veuillez réessayer plus tard.");
        }

        // Récupérer le token
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Token invalide"));

        // Vérifier si déjà utilisé
        if (resetToken.isUsed()) {
            tokenRepository.delete(resetToken);
            throw new TokenAlreadyUsedException("Ce token a déjà été utilisé");
        }

        // Vérifier l'expiration
        if (Instant.now().isAfter(resetToken.getExpiration())) {
            tokenRepository.delete(resetToken);
            throw new TokenExpiredException("Le token a expiré. Veuillez refaire une demande.");
        }

        // Récupérer l'utilisateur
        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new SamePasswordException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        if (!isValidPassword(newPassword)) {
            throw new WeakPasswordException("Le mot de passe ne respecte pas les règles de sécurité");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);

        notificationService.envoyerConfirmationMotDePasseModifie(user.getEmail(), user.getNom());

        return buildSuccessResponse(HttpStatus.ACCEPTED, "Mot de passe réinitialisé avec succès. Vous pouvez maintenant vous connecter.", "PASSWORD_RESET_SUCCESS", null);

    }


    @Transactional
    public ResponseEntity<?> resendResetToken(String email, HttpServletRequest request) {
        if (rateLimitService.estLimiteDepassee("resend-reset:" + extraireIp(request))) {
            throw new TooManyRequestsException("Trop de tentatives. Veuillez réessayer plus tard.");
        }

        PasswordResetToken existingToken = tokenRepository.findByEmail(email).orElse(null);
        if (existingToken == null) {
            return reponseNeutre();
        }

        if (existingToken.getAttemptCount() >= MAX_RESEND_ATTEMPTS) {
            tokenRepository.delete(existingToken);
            throw new RuntimeException("Trop de tentatives. Veuillez refaire une demande.");
        }

        // Vérifier le délai minimum entre deux envois
        long minutesSinceLast = java.time.Duration.between(existingToken.getCreation(), Instant.now()).toMinutes();
        if (minutesSinceLast < RESEND_COOLDOWN_MINUTES) {
            throw new RuntimeException("Veuillez attendre " + RESEND_COOLDOWN_MINUTES +
                    " minutes avant de demander un nouveau code");
        }

        String newToken = generateResetToken();
        existingToken.setToken(newToken);
        existingToken.setCreation(Instant.now());
        existingToken.setExpiration(Instant.now().plus(TOKEN_EXPIRATION_MINUTES, MINUTES));
        existingToken.setAttemptCount(existingToken.getAttemptCount() + 1);

        tokenRepository.upsertToken(existingToken);

        userRepository.findByEmail(email).ifPresent(user -> notificationService.envoyerCodeReinitialisation(email, user.getNom(), newToken));

        return reponseNeutre();
    }

    private String generateResetToken() {
        return String.format("%06d", SECURE_RANDOM.nextInt(999999));
    }

    private String extraireIp(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }

    private boolean isValidPassword(String password) {
        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$";
        return password != null && password.matches(passwordRegex);
    }

    @Transactional
    public void cleanExpiredPassordResetToken() {
        tokenRepository.deleteAllExpired(Instant.now());
    }
}