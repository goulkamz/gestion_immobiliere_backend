package com.immobilier.gestionImmobiliere.modules.annonces.controllers;

import com.immobilier.gestionImmobiliere.modules.annonces.apis.ContactPublicAPI;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateContactDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.services.ContactService;
import org.springframework.http.ResponseEntity;

public class ContactPublicController implements ContactPublicAPI {

    private final ContactService contactService;

    public ContactPublicController(ContactService contactService) {
        this.contactService = contactService;
    }

    @Override // public — F20, formulaire de contact général
    public ResponseEntity<?> create(CreateContactDTO dto) {
        return contactService.create(dto);
    }

}
