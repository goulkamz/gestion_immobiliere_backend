package com.immobilier.gestionImmobiliere.modules.annonces.apis;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutContact;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateContactDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutContactDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/public/contacts")
public interface ContactAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) StatutContact statut, Pageable pageable);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateContactDTO dto);

    @PatchMapping("/{id}/statut")
    ResponseEntity<?> updateStatut(@PathVariable Integer id, @Valid @RequestBody UpdateStatutContactDTO dto);
}