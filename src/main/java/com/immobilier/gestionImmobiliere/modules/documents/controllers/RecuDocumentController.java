package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.RecuDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.RecuDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecuDocumentController implements RecuDocumentAPI {

    private final RecuDocumentService recuDocumentService;

    public RecuDocumentController(RecuDocumentService recuDocumentService) {
        this.recuDocumentService = recuDocumentService;
    }

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')" +
            "or @paiementOwnershipResolver.isPaiementAccessible(#idPaiement, authentication.principal.idUser, false) " +
            "or @paiementOwnershipResolver.isPaiementAccessible(#idPaiement, authentication.principal.idUser, true)" +
            "or @paiementOwnershipResolver.isPaiementAccessibleBienService(#idPaiement, authentication.principal.idUser)")
    @Override
    public ResponseEntity<byte[]> genererOuRecuperer(Integer idPaiement, UserDetailsImpl currentUser) {
        byte[] pdf = recuDocumentService.genererOuRecuperer(idPaiement, currentUser.getIdUser());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("recu.pdf").build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}