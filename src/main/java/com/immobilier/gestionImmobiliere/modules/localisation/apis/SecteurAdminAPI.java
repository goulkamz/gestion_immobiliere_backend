package com.immobilier.gestionImmobiliere.modules.localisation.apis;

import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.CreateSecteurDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.UpdateSecteurDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Administration du référentiel, réservée à l'admin/agent
@RequestMapping("/api/secteurs")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public interface SecteurAdminAPI {

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateSecteurDTO dto);

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateSecteurDTO dto);

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}