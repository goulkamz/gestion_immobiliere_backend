package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.ReglementDepenseRecuDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.ReglementDepenseRecuDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Admin, agent et bailleur (ses propres cours, vérifié dans le service). */
@RestController
@PreAuthorize("hasAnyRole('ADMIN','AGENT','BAILLEUR')")
public class ReglementDepenseRecuDocumentController implements ReglementDepenseRecuDocumentAPI {

    private final ReglementDepenseRecuDocumentService service;

    public ReglementDepenseRecuDocumentController(ReglementDepenseRecuDocumentService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<byte[]> genererOuRecuperer(Integer idReglement, UserDetailsImpl currentUser) {
        byte[] pdf = service.genererOuRecuperer(idReglement, currentUser.getIdUser(), currentUser.hasAnyRole("ADMIN", "AGENT"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("recu-reglement.pdf").build());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(pdf);
    }
}
