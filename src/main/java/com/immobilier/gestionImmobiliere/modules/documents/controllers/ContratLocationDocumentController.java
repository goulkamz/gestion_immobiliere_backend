package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.ContratLocationDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.ContratLocationDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContratLocationDocumentController implements ContratLocationDocumentAPI {

    private final ContratLocationDocumentService contratLocationDocumentService;

    public ContratLocationDocumentController(ContratLocationDocumentService contratLocationDocumentService) {
        this.contratLocationDocumentService = contratLocationDocumentService;
    }

    @Override
    public ResponseEntity<?> genererOuRecuperer(Integer idContrat, UserDetailsImpl currentUser) {
        return contratLocationDocumentService.genererOuRecuperer(idContrat, currentUser.getIdUser());
    }
}