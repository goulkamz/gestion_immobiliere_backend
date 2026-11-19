package com.immobilier.gestionImmobiliere.modules.depenses.apis;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDevis;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AccepterDevisDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.CreateDevisDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.DepenseDepuisDevisDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.ReponseDevisDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.UpdateDevisDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AccordHorsPlateformeDTO;

@RequestMapping("/api/devis")
public interface DevisAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idCour,
                             @RequestParam(required = false) StatutDevis statut,
                             @RequestParam(required = false) String recherche,
                             Pageable pageable,
                             @AuthenticationPrincipal UserDetailsImpl currentUser);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);

    // ---- Agence ----

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    /** Correction avant réponse, ou révision après une demande de renégociation (soumis à nouveau). */
    @PatchMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/annuler")
    ResponseEntity<?> annuler(@PathVariable Integer id, @Valid @RequestBody ReponseDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    /** Enregistre la dépense d'un devis accepté. */
    @PostMapping("/{id}/depense")
    ResponseEntity<?> enregistrerDepense(@PathVariable Integer id, @Valid @RequestBody(required = false) DepenseDepuisDevisDTO dto,
                                         @AuthenticationPrincipal UserDetailsImpl currentUser);

    /** Accord donné par le bailleur hors plateforme, enregistré par l'agent (qui, quand, comment). */
    @PatchMapping("/{id}/accord-hors-plateforme")
    ResponseEntity<?> enregistrerAccordHorsPlateforme(@PathVariable Integer id, @Valid @RequestBody AccordHorsPlateformeDTO dto,
                                                      @AuthenticationPrincipal UserDetailsImpl currentUser);

    /** Joint (ou remplace) le devis PDF de l'artisan. */
    @PostMapping(value = "/{id}/piece-jointe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<?> joindrePieceJointe(@PathVariable Integer id, @RequestPart("fichier") MultipartFile fichier,
                                         @AuthenticationPrincipal UserDetailsImpl currentUser);

    @DeleteMapping("/{id}/piece-jointe")
    ResponseEntity<?> retirerPieceJointe(@PathVariable Integer id);

    /** Devis PDF joint : agence et bailleur de la cour. */
    @GetMapping(value = "/{id}/piece-jointe", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> telechargerPieceJointe(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);

    // ---- Bailleur ----

    /** Le bailleur ne reconnaît pas l'accord enregistré hors plateforme par l'agence. */
    @PatchMapping("/{id}/contester-accord")
    ResponseEntity<?> contesterAccord(@PathVariable Integer id, @Valid @RequestBody ReponseDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/accepter")
    ResponseEntity<?> accepter(@PathVariable Integer id, @Valid @RequestBody AccepterDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/renegocier")
    ResponseEntity<?> renegocier(@PathVariable Integer id, @Valid @RequestBody ReponseDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/rejeter")
    ResponseEntity<?> rejeter(@PathVariable Integer id, @Valid @RequestBody ReponseDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);
}
