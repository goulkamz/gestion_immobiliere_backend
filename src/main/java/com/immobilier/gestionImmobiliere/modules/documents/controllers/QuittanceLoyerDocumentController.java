package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.QuittanceLoyerDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.QuittanceLoyerDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuittanceLoyerDocumentController implements QuittanceLoyerDocumentAPI {

    private final QuittanceLoyerDocumentService quittanceLoyerDocumentService;

    public QuittanceLoyerDocumentController(QuittanceLoyerDocumentService quittanceLoyerDocumentService) {
        this.quittanceLoyerDocumentService = quittanceLoyerDocumentService;
    }

    @Override
    public ResponseEntity<?> genererOuRecuperer(Integer idEcheance, UserDetailsImpl currentUser) {
        return quittanceLoyerDocumentService.genererOuRecuperer(idEcheance, currentUser.getIdUser());
    }
}