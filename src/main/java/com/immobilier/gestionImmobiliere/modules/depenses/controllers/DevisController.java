package com.immobilier.gestionImmobiliere.modules.depenses.controllers;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDevis;
import com.immobilier.gestionImmobiliere.modules.depenses.apis.DevisAPI;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AccepterDevisDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.CreateDevisDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.DepenseDepuisDevisDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.ReponseDevisDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.UpdateDevisDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AccordHorsPlateformeDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.services.DevisPieceJointeService;
import com.immobilier.gestionImmobiliere.modules.depenses.services.DevisService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lecture : admin, agent et bailleur (ses cours uniquement, filtré dans le service).
 * Soumission, révision, retrait et enregistrement de la dépense : admin et agent.
 * Réponse (accepter, renégocier, rejeter) : le bailleur propriétaire de la cour, vérifié dans le service.
 */
@RestController
@PreAuthorize("hasAnyRole('ADMIN','AGENT','BAILLEUR')")
public class DevisController implements DevisAPI {

    private final DevisService devisService;
    private final DevisPieceJointeService pieceJointeService;

    public DevisController(DevisService devisService, DevisPieceJointeService pieceJointeService) {
        this.devisService = devisService;
        this.pieceJointeService = pieceJointeService;
    }

    @Override
    public ResponseEntity<?> getAll(Integer idCour, StatutDevis statut, String recherche, Pageable pageable, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return devisService.getAllForCurrentUser(idCour, statut, recherche, currentUser.getIdUser(), currentUser.hasAnyRole("ADMIN", "AGENT"), pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return devisService.getByIdForCurrentUser(id, currentUser.getIdUser(), currentUser.hasAnyRole("ADMIN", "AGENT"));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> create(CreateDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return devisService.create(dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> update(Integer id, UpdateDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return devisService.update(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> annuler(Integer id, ReponseDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return devisService.annuler(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> enregistrerDepense(Integer id, DepenseDepuisDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return devisService.enregistrerDepense(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> enregistrerAccordHorsPlateforme(Integer id, AccordHorsPlateformeDTO dto,
                                                             @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return devisService.enregistrerAccordHorsPlateforme(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> joindrePieceJointe(Integer id, MultipartFile fichier, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return pieceJointeService.joindre(id, fichier, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> retirerPieceJointe(Integer id) {
        return pieceJointeService.retirer(id);
    }

    @Override
    public ResponseEntity<byte[]> telechargerPieceJointe(Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        byte[] pdf = pieceJointeService.telecharger(id, currentUser.getIdUser(), currentUser.hasAnyRole("ADMIN", "AGENT"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.inline().filename("devis-" + id + ".pdf").build());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    @Override
    @PreAuthorize("hasRole('BAILLEUR')")
    public ResponseEntity<?> contesterAccord(Integer id, ReponseDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return devisService.contesterAccord(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('BAILLEUR')")
    public ResponseEntity<?> accepter(Integer id, AccepterDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return devisService.accepter(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('BAILLEUR')")
    public ResponseEntity<?> renegocier(Integer id, ReponseDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return devisService.renegocier(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('BAILLEUR')")
    public ResponseEntity<?> rejeter(Integer id, ReponseDevisDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return devisService.rejeter(id, dto, currentUser.getIdUser());
    }
}
