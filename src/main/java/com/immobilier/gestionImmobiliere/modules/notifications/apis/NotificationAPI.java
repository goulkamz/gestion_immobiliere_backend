package com.immobilier.gestionImmobiliere.modules.notifications.apis;

import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Notifications de l'utilisateur authentifie uniquement (tous roles) : aucun userId n'est accepte en parametre
@RequestMapping("/api/notifications")
public interface NotificationAPI {

    @GetMapping
    ResponseEntity<?> getMesNotifications(@RequestParam(defaultValue = "false") boolean nonLuesSeulement,
                                          @PageableDefault(size = 20, sort = "idNotification", direction = Sort.Direction.DESC) Pageable pageable,
                                          @AuthenticationPrincipal UserDetailsImpl currentUser);

    @GetMapping("/non-lues/count")
    ResponseEntity<?> countNonLues(@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/lue")
    ResponseEntity<?> marquerLue(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/lues")
    ResponseEntity<?> marquerToutesLues(@AuthenticationPrincipal UserDetailsImpl currentUser);
}
