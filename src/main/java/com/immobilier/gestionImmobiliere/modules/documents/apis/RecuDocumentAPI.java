package com.immobilier.gestionImmobiliere.modules.documents.apis;

import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/documents/recus")
public interface RecuDocumentAPI {


    @GetMapping("/{idPaiement}")
    ResponseEntity<?> genererOuRecuperer(@PathVariable Integer idPaiement, @AuthenticationPrincipal UserDetailsImpl currentUser);
}