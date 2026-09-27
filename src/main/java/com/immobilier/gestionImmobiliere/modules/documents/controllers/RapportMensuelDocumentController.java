package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.RapportMensuelDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.RapportMensuelDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class RapportMensuelDocumentController implements RapportMensuelDocumentAPI {

    private final RapportMensuelDocumentService rapportMensuelDocumentService;

    public RapportMensuelDocumentController(RapportMensuelDocumentService rapportMensuelDocumentService) {
        this.rapportMensuelDocumentService = rapportMensuelDocumentService;
    }


    @Override
    public ResponseEntity<byte[]> genererOuRecuperer(LocalDate periode, UserDetailsImpl currentUser) {
        byte[] pdf = rapportMensuelDocumentService.genererOuRecuperer(periode, currentUser.getIdUser());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("rapport-mensuel.pdf").build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}