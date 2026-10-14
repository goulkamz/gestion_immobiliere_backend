package com.immobilier.gestionImmobiliere.modules.notifications.controllers;

import com.immobilier.gestionImmobiliere.modules.notifications.apis.NotificationAPI;
import com.immobilier.gestionImmobiliere.modules.notifications.services.NotificationInAppService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("isAuthenticated()")
public class NotificationController implements NotificationAPI {

    private final NotificationInAppService notificationService;

    public NotificationController(NotificationInAppService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public ResponseEntity<?> getMesNotifications(boolean nonLuesSeulement, Pageable pageable, UserDetailsImpl currentUser) {
        return notificationService.getMesNotifications(currentUser.getIdUser(), nonLuesSeulement, pageable);
    }

    @Override
    public ResponseEntity<?> countNonLues(UserDetailsImpl currentUser) {
        return notificationService.countNonLues(currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> marquerLue(Integer id, UserDetailsImpl currentUser) {
        return notificationService.marquerLue(id, currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> marquerToutesLues(UserDetailsImpl currentUser) {
        return notificationService.marquerToutesLues(currentUser.getIdUser());
    }
}
