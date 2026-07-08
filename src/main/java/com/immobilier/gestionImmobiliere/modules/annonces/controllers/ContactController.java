package com.immobilier.gestionImmobiliere.modules.annonces.controllers;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutContact;
import com.immobilier.gestionImmobiliere.modules.annonces.apis.ContactAPI;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateContactDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutContactDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.services.ContactService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContactController implements ContactAPI {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> getAll(StatutContact statut, Pageable pageable) {
        return contactService.getAll(statut, pageable);
    }

    @Override // public — F20, formulaire de contact général
    public ResponseEntity<?> create(CreateContactDTO dto) {
        return contactService.create(dto);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutContactDTO dto) {
        return contactService.updateStatut(id, dto);
    }
}