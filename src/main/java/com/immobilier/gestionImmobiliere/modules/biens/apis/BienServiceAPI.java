package com.immobilier.gestionImmobiliere.modules.biens.apis;

import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateBienServiceDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateBienServiceDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateDisponibiliteBienServiceDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/biens-services")
public interface BienServiceAPI {

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateBienServiceDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateBienServiceDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PatchMapping("/{id}/disponibilite")
    ResponseEntity<?> updateDisponibilite(@PathVariable Integer id, @Valid @RequestBody UpdateDisponibiliteBienServiceDTO dto);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}