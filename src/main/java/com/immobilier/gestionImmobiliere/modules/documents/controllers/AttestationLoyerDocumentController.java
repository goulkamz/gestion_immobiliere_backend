package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.AttestationLoyerDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.AttestationLoyerDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AttestationLoyerDocumentController implements AttestationLoyerDocumentAPI {

    private final AttestationLoyerDocumentService attestationLoyerDocumentService;

    public AttestationLoyerDocumentController(AttestationLoyerDocumentService attestationLoyerDocumentService) {
        this.attestationLoyerDocumentService = attestationLoyerDocumentService;
    }

    @Override
    public ResponseEntity<byte[]> genererAttestation(Integer idContrat, UserDetailsImpl currentUser) {
        byte[] pdf = attestationLoyerDocumentService.genererAttestation(idContrat);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("attestation-loyer.pdf").build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}