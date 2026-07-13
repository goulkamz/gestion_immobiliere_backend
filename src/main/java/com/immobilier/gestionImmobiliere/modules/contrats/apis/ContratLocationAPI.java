package com.immobilier.gestionImmobiliere.modules.contrats.apis;

import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.CreateContratLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.TerminerLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.responses.ContratLocationResponseDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
    ResponseEntity<?> terminer(@PathVariable Integer id, @Valid @RequestBody TerminerLocationDTO dto,@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/resilier")
    ResponseEntity<?> resilier(@PathVariable Integer id,@AuthenticationPrincipal UserDetailsImpl currentUser);
}