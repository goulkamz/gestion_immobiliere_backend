package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.AvenantDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.AvenantDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AvenantDocumentController implements AvenantDocumentAPI {

    private final AvenantDocumentService avenantDocumentService;

    public AvenantDocumentController(AvenantDocumentService avenantDocumentService) {
        this.avenantDocumentService = avenantDocumentService;
    }

    @Override
    public ResponseEntity<byte[]> genererOuRecuperer(Integer idAvenant, UserDetailsImpl currentUser) {
        byte[] pdf = avenantDocumentService.genererOuRecuperer(idAvenant, currentUser.getIdUser());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("avenant.pdf").build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
