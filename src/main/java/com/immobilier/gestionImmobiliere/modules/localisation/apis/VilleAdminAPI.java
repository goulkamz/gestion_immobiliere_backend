package com.immobilier.gestionImmobiliere.modules.localisation.apis;

import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.CreateVilleDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.UpdateVilleDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Administration du référentiel : create/update réservés à ADMIN+AGENT, delete réservé à ADMIN
@RequestMapping("/api/villes")
public interface VilleAdminAPI {

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateVilleDTO dto);

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateVilleDTO dto);

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}