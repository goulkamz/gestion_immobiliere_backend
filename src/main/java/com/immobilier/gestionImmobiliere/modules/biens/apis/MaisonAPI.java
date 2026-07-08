package com.immobilier.gestionImmobiliere.modules.biens.apis;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateStatutMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/maisons")
public interface MaisonAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idCour,
                             @RequestParam(required = false) StatutMaison statut,
                             Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateMaisonDTO dto);

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateMaisonDTO dto);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateMaisonDTO dto,@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateMaisonDTO dto,@AuthenticationPrincipal UserDetailsImpl currentUser);


    @PatchMapping("/{id}/statut")
    ResponseEntity<?> updateStatut(@PathVariable Integer id, @Valid @RequestBody UpdateStatutMaisonDTO dto,@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/statut")
    ResponseEntity<?> updateStatut(@PathVariable Integer id, @Valid @RequestBody UpdateStatutMaisonDTO dto);


    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}