package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.ContratMandatDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.ContratMandatDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContratMandatDocumentController implements ContratMandatDocumentAPI {

    private final ContratMandatDocumentService contratMandatDocumentService;

    public ContratMandatDocumentController(ContratMandatDocumentService contratMandatDocumentService) {
        this.contratMandatDocumentService = contratMandatDocumentService;
    }

    @Override
    public ResponseEntity<?> genererOuRecuperer(Integer idMandat, UserDetailsImpl currentUser) {
        return contratMandatDocumentService.genererOuRecuperer(idMandat, currentUser.getIdUser());
    }
}