package com.immobilier.gestionImmobiliere.modules.depenses.apis;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDepense;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AbandonnerDepenseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AnnulerDepenseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AnnulerReglementDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.CreateDepenseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.UpdateDepenseDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/depenses")
public interface DepenseAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idCour,
                             @RequestParam(required = false) Integer idMaison,
                             @RequestParam(required = false) Integer idMandat,
                             @RequestParam(required = false) StatutDepense statut,
                             @RequestParam(required = false) String recherche,
                             Pageable pageable,
                             @AuthenticationPrincipal UserDetailsImpl currentUser);

    /** Mandats actifs dont la date de fin est dépassée et qui portent encore des dépenses en cours. */
    @GetMapping("/mandats-echus")
    ResponseEntity<?> getMandatsEchus();

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);

    /** Retenues faites sur un reversement (échéance MANDAT). */
    @GetMapping("/reversements/{idEcheance}")
    ResponseEntity<?> getImputationsReversement(@PathVariable Integer idEcheance, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateDepenseDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateDepenseDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/annuler")
    ResponseEntity<?> annuler(@PathVariable Integer id, @Valid @RequestBody AnnulerDepenseDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    /** Nouvel étalement du solde restant d'une dépense en cours. */
    @PatchMapping("/{id}/reechelonner")
    ResponseEntity<?> reechelonner(@PathVariable Integer id, @Valid @RequestBody com.immobilier.gestionImmobiliere.modules.depenses.dto.ReechelonnerDepenseDTO dto,
                                   @AuthenticationPrincipal UserDetailsImpl currentUser);

    /** Règlement direct du bailleur pour une dépense due après la résiliation du mandat. */
    @PostMapping("/{id}/reglements")
    ResponseEntity<?> enregistrerReglement(@PathVariable Integer id, @Valid @RequestBody com.immobilier.gestionImmobiliere.modules.depenses.dto.ReglementDepenseDTO dto,
                                           @AuthenticationPrincipal UserDetailsImpl currentUser);

    /** Annule un règlement saisi par erreur (il reste en base, marqué annulé). */
    @PatchMapping("/{id}/reglements/{idReglement}/annuler")
    ResponseEntity<?> annulerReglement(@PathVariable Integer id, @PathVariable Integer idReglement,
                                       @Valid @RequestBody AnnulerReglementDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    /** Rétablit une créance abandonnée par erreur (admin). */
    @PatchMapping("/{id}/abandon/retablir")
    ResponseEntity<?> retablirCreance(@PathVariable Integer id, @Valid @RequestBody com.immobilier.gestionImmobiliere.modules.depenses.dto.MotifDTO dto,
                                      @AuthenticationPrincipal UserDetailsImpl currentUser);

    /** Le bailleur conteste une dépense : signalée à l'agence, rien n'est annulé automatiquement. */
    @PatchMapping("/{id}/contester")
    ResponseEntity<?> contester(@PathVariable Integer id, @Valid @RequestBody com.immobilier.gestionImmobiliere.modules.depenses.dto.MotifDTO dto,
                                @AuthenticationPrincipal UserDetailsImpl currentUser);

    /** L'agence répond à la contestation du bailleur et la clôt. */
    @PatchMapping("/{id}/contestation/repondre")
    ResponseEntity<?> repondreContestation(@PathVariable Integer id, @Valid @RequestBody com.immobilier.gestionImmobiliere.modules.depenses.dto.MotifDTO dto,
                                           @AuthenticationPrincipal UserDetailsImpl currentUser);

    /** Abandon de créance sur le reste dû d'une dépense (admin). */
    @PatchMapping("/{id}/abandon")
    ResponseEntity<?> abandonner(@PathVariable Integer id, @Valid @RequestBody AbandonnerDepenseDTO dto,
                                 @AuthenticationPrincipal UserDetailsImpl currentUser);
}
