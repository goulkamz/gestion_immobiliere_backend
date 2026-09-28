package com.immobilier.gestionImmobiliere.modules.localisation.apis;

import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.CreatePaysDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.UpdatePaysDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Administration du référentiel : create/update réservés à ADMIN+AGENT, delete réservé à ADMIN
@RequestMapping("/api/pays")
public interface PaysAdminAPI {

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreatePaysDTO dto);

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdatePaysDTO dto);

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}