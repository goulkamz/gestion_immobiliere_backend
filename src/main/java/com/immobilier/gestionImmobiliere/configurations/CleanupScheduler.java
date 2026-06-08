package com.immobilier.gestionImmobiliere.configurations;

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

    // Nettoyage toutes les heures
    @Scheduled(fixedRate = 3600000)
    public void cleanExpiredPendingRegistrations() {
        userService.cleanExpiredPendingRegistrations();
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanExpiredPasswordResetToken() {
        passwordResetService.cleanExpiredPassordResetToken();
    }
}