package com.immobilier.gestionImmobiliere.modules.user.deuxfacteurs;

import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

/** Gestion de la double authentification par l'utilisateur connecté, et réinitialisation par l'admin. */
@RestController
@PreAuthorize("isAuthenticated()")
public class DeuxFacteursController {

    public record CodeDTO(@NotBlank(message = "Le code est obligatoire") String code) {}

    public record DesactivationDTO(@NotBlank(message = "Le mot de passe est obligatoire") String motDePasse,
                                   @NotBlank(message = "Le code est obligatoire") String code) {}

    private final DeuxFacteursService service;

    public DeuxFacteursController(DeuxFacteursService service) {
        this.service = service;
    }

    @GetMapping("/api/users/me/2fa")
    public ResponseEntity<?> statut(@AuthenticationPrincipal UserDetailsImpl user) {
        return buildSuccessResponse(HttpStatus.OK, "Statut de la double authentification", "TWO_FACTOR_STATUS",
                service.statut(user.getIdUser()));
    }

    @PostMapping("/api/users/me/2fa/initier")
    public ResponseEntity<?> initier(@AuthenticationPrincipal UserDetailsImpl user) {
        return buildSuccessResponse(HttpStatus.OK, "Scannez le QR code avec votre application", "TWO_FACTOR_SETUP",
                service.initier(user.getIdUser()));
    }

    @PostMapping("/api/users/me/2fa/confirmer")
    public ResponseEntity<?> confirmer(@Valid @RequestBody CodeDTO dto, @AuthenticationPrincipal UserDetailsImpl user) {
        return buildSuccessResponse(HttpStatus.OK, "Double authentification activée", "TWO_FACTOR_ENABLED",
                Map.of("codesSecours", service.confirmer(user.getIdUser(), dto.code())));
    }

    @PostMapping("/api/users/me/2fa/desactiver")
    public ResponseEntity<?> desactiver(@Valid @RequestBody DesactivationDTO dto,
                                        @AuthenticationPrincipal UserDetailsImpl user) {
        service.desactiver(user.getIdUser(), dto.motDePasse(), dto.code());
        return buildSuccessResponse(HttpStatus.OK, "Double authentification désactivée", "TWO_FACTOR_DISABLED", null);
    }

    @PostMapping("/api/users/me/2fa/codes-secours")
    public ResponseEntity<?> regenererCodes(@Valid @RequestBody CodeDTO dto,
                                            @AuthenticationPrincipal UserDetailsImpl user) {
        return buildSuccessResponse(HttpStatus.OK, "Nouveaux codes de secours générés", "TWO_FACTOR_BACKUP_CODES",
                Map.of("codesSecours", service.regenererCodesSecours(user.getIdUser(), dto.code())));
    }

    @DeleteMapping("/api/admin/users/{id}/2fa")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reinitialiser(@PathVariable Integer id) {
        service.reinitialiserParAdmin(id);
        return buildSuccessResponse(HttpStatus.OK, "Double authentification réinitialisée", "TWO_FACTOR_RESET", null);
    }
}
