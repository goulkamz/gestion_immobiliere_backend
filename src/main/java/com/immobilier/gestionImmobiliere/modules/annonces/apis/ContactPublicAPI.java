package com.immobilier.gestionImmobiliere.modules.annonces.apis;

import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateContactDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Soumission anonyme du formulaire de contact (F20)
@RequestMapping("/api/public/contacts")
public interface ContactPublicAPI {

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateContactDTO dto);
}