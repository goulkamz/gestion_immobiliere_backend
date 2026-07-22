package com.immobilier.gestionImmobiliere.modules.biens.apis;

import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateCourDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateCourDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/cours")
public interface CourAPI {

    // Ouvert à AGENT/ADMIN/BAILLEUR : le service DOIT filtrer par id_user si role = BAILLEUR
    @PreAuthorize("hasAnyRole('AGENT','ADMIN','BAILLEUR')")
    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idSecteur, Pageable pageable,
                             @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN','BAILLEUR')")
    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateCourDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateCourDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}