package com.immobilier.gestionImmobiliere.modules.contrats.apis;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.CreateContratMandatDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.ResilierMandatDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/contrats-mandat")
public interface ContratMandatAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idCour,
                             @RequestParam(required = false) StatutMandat statut,
                             Pageable pageable,@AuthenticationPrincipal UserDetailsImpl currentUser);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id,@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateContratMandatDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/activer")
    ResponseEntity<?> activer(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/resilier")
    ResponseEntity<?> resilier(@PathVariable Integer id, @Valid @RequestBody ResilierMandatDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}