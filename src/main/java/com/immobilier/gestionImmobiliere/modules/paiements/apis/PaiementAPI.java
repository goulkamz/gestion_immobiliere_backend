package com.immobilier.gestionImmobiliere.modules.paiements.apis;

import com.immobilier.gestionImmobiliere.modules.paiements.dto.requests.CreatePaiementDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/paiements")
public interface PaiementAPI {

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreatePaiementDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);
}