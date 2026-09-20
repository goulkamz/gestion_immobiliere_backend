package com.immobilier.gestionImmobiliere.modules.documents.apis;

import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@RequestMapping("/api/documents/rapports-mensuels")
public interface RapportMensuelDocumentAPI {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    ResponseEntity<?> genererOuRecuperer(@RequestParam LocalDate periode, @AuthenticationPrincipal UserDetailsImpl currentUser);
}