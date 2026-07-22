package com.immobilier.gestionImmobiliere.modules.annonces.apis;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutContact;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutContactDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Consultation/traitement des messages, réservé au staff
@RequestMapping("/api/contacts")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public interface ContactAdminAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) StatutContact statut, Pageable pageable);

    @PatchMapping("/{id}/statut")
    ResponseEntity<?> updateStatut(@PathVariable Integer id, @Valid @RequestBody UpdateStatutContactDTO dto);
}