package com.immobilier.gestionImmobiliere.configurations;

import com.immobilier.gestionImmobiliere.modules.medias.services.MediaReconciliationService;
import com.immobilier.gestionImmobiliere.modules.user.jwt.JwtUtils;
import com.immobilier.gestionImmobiliere.modules.user.services.PasswordResetService;
import com.immobilier.gestionImmobiliere.modules.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class CleanupScheduler {

    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final JwtUtils jwtUtils;
    private final MediaReconciliationService mediaReconciliationService;

    // Nettoyage tous les jours a 2h du matin
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanExpiredPendingRegistrations() {
        userService.cleanExpiredPendingRegistrations();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void cleanExpiredPasswordResetToken() {
        passwordResetService.cleanExpiredPassordResetToken();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void cleanExpiredToken() {
        jwtUtils.cleanupExpiredTokens();
    }

    @Scheduled(cron = "0 0 3 * * SUN") // dimanche 3h du matin, hors heures de charge
    public void nettoyerFichiersOrphelins() {
        mediaReconciliationService.nettoyerFichiersOrphelins();
    }
}