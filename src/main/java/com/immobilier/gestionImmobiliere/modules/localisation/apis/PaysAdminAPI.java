package com.immobilier.gestionImmobiliere.modules.localisation.apis;

import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.CreatePaysDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.UpdatePaysDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Administration du référentiel, réservée à l'admin
@RequestMapping("/api/pays")
@PreAuthorize("hasRole('ADMIN')")
public interface PaysAdminAPI {

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreatePaysDTO dto);

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdatePaysDTO dto);

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}