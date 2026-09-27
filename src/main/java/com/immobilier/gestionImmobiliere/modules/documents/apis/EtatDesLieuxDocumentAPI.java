package com.immobilier.gestionImmobiliere.modules.documents.apis;

import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/documents/etats-des-lieux")
public interface EtatDesLieuxDocumentAPI {

    @GetMapping(value = "/{idContrat}/entree", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> genererEntree(@PathVariable Integer idContrat, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @GetMapping(value = "/{idContrat}/sortie", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> genererSortie(@PathVariable Integer idContrat, @AuthenticationPrincipal UserDetailsImpl currentUser);
}