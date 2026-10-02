package com.immobilier.gestionImmobiliere.modules.contrats.apis;

import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.CreateContratLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.ResilierLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.TerminerLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.UpdateContratLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.responses.ContratLocationResponseDTO;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.requests.ConfirmerReglementSortieDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/contrats-location")
public interface ContratLocationAPI {


    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idMaison,
                             @RequestParam(required = false) Integer idLocataire,
                             Pageable pageable,
                             @AuthenticationPrincipal UserDetailsImpl currentUser);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);

    @PostMapping
    ContratLocationResponseDTO create(@Valid @RequestBody CreateContratLocationDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/terminer")
    ResponseEntity<?> terminer(@PathVariable Integer id, @Valid @RequestBody TerminerLocationDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    // Modifie dateSortie (prolonge/réduit le bail) et/ou montantLoyer sans clôturer
    // le contrat — à distinguer de terminer()/resilierContratLocation() qui mettent
    // fin au bail. Réservé aux contrats ACTIF.
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PatchMapping("/{id}/modifier")
    ResponseEntity<?> modifier(@PathVariable Integer id, @Valid @RequestBody UpdateContratLocationDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);



    @PatchMapping("/{id}/resilier")
    ResponseEntity<?> resilierContratLocation(@PathVariable Integer id, @Valid @RequestBody ResilierLocationDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PatchMapping("/decomptes-sortie/{idDecompte}/regler")
    ResponseEntity<?> reglerDecompteSortie(@PathVariable Integer idDecompte, @Valid @RequestBody ConfirmerReglementSortieDTO dto,
                                           @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @GetMapping("/{id}/decompte-sortie")
    ResponseEntity<?> getDecompteSortie(@PathVariable Integer id);
}