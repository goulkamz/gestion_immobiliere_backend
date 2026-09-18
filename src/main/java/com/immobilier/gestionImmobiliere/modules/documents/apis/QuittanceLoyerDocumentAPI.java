package com.immobilier.gestionImmobiliere.modules.documents.apis;

import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/documents/quittances")
public interface QuittanceLoyerDocumentAPI {

    @GetMapping("/{idEcheance}")
    ResponseEntity<?> genererOuRecuperer(@PathVariable Integer idEcheance, @AuthenticationPrincipal UserDetailsImpl currentUser);
}