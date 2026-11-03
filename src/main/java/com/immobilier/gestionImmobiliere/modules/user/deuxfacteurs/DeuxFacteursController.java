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

    /**
     * Corps d'une requête portant un code à 6 chiffres saisi par l'utilisateur.
     */
    public record CodeDTO(@NotBlank(message = "Le code est obligatoire") String code) {}

    /**
     * Corps de la désactivation : mot de passe et code courant, pour qu'une session volée ne puisse pas désactiver la 2FA.
     */
    public record DesactivationDTO(@NotBlank(message = "Le mot de passe est obligatoire") String motDePasse,
                                   @NotBlank(message = "Le code est obligatoire") String code) {}

    private final DeuxFacteursService service;

    /**
     * Reçoit le service qui porte toute la logique ; le contrôleur ne fait que relayer et mettre en forme la réponse.
     */
    public DeuxFacteursController(DeuxFacteursService service) {
        this.service = service;
    }

    /**
     * GET /api/users/me/2fa : état de la 2FA de l'utilisateur connecté (disponible, active, codes de secours restants, durée de confiance).
     */
    @GetMapping("/api/users/me/2fa")
    public ResponseEntity<?> statut(@AuthenticationPrincipal UserDetailsImpl user) {
        return buildSuccessResponse(HttpStatus.OK, "Statut de la double authentification", "TWO_FACTOR_STATUS",
                service.statut(user.getIdUser()));
    }

    /**
     * POST /api/users/me/2fa/initier : génère un secret et renvoie le QR code à scanner ; la 2FA n'est pas encore active.
     */
    @PostMapping("/api/users/me/2fa/initier")
    public ResponseEntity<?> initier(@AuthenticationPrincipal UserDetailsImpl user) {
        return buildSuccessResponse(HttpStatus.OK, "Scannez le QR code avec votre application", "TWO_FACTOR_SETUP",
                service.initier(user.getIdUser()));
    }

    /**
     * POST /api/users/me/2fa/confirmer : vérifie le premier code, active la 2FA et renvoie les codes de secours (visibles une seule fois).
     */
    @PostMapping("/api/users/me/2fa/confirmer")
    public ResponseEntity<?> confirmer(@Valid @RequestBody CodeDTO dto, @AuthenticationPrincipal UserDetailsImpl user) {
        return buildSuccessResponse(HttpStatus.OK, "Double authentification activée", "TWO_FACTOR_ENABLED",
                Map.of("codesSecours", service.confirmer(user.getIdUser(), dto.code())));
    }

    /**
     * POST /api/users/me/2fa/desactiver : désactive la 2FA sur présentation du mot de passe et d'un code valide.
     */
    @PostMapping("/api/users/me/2fa/desactiver")
    public ResponseEntity<?> desactiver(@Valid @RequestBody DesactivationDTO dto,
                                        @AuthenticationPrincipal UserDetailsImpl user) {
        service.desactiver(user.getIdUser(), dto.motDePasse(), dto.code());
        return buildSuccessResponse(HttpStatus.OK, "Double authentification désactivée", "TWO_FACTOR_DISABLED", null);
    }

    /**
     * POST /api/users/me/2fa/codes-secours : remplace les codes de secours par 10 nouveaux, sur présentation d'un code valide.
     */
    @PostMapping("/api/users/me/2fa/codes-secours")
    public ResponseEntity<?> regenererCodes(@Valid @RequestBody CodeDTO dto,
                                            @AuthenticationPrincipal UserDetailsImpl user) {
        return buildSuccessResponse(HttpStatus.OK, "Nouveaux codes de secours générés", "TWO_FACTOR_BACKUP_CODES",
                Map.of("codesSecours", service.regenererCodesSecours(user.getIdUser(), dto.code())));
    }

    /**
     * DELETE /api/admin/users/{id}/2fa (admin) : désactive la 2FA d'un membre du personnel (téléphone perdu) et coupe ses sessions.
     */
    @DeleteMapping("/api/admin/users/{id}/2fa")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reinitialiser(@PathVariable Integer id) {
        service.reinitialiserParAdmin(id);
        return buildSuccessResponse(HttpStatus.OK, "Double authentification réinitialisée", "TWO_FACTOR_RESET", null);
    }
}
