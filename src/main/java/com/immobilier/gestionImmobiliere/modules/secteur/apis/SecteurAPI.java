package com.immobilier.gestionImmobiliere.modules.secteur.apis;

import com.immobilier.gestionImmobiliere.modules.secteur.dto.requests.CreateSecteurDTO;
import com.immobilier.gestionImmobiliere.modules.secteur.dto.requests.UpdateSecteurDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/secteurs")
public interface SecteurAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Long idVille, Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Long id);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateSecteurDTO dto);

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateSecteurDTO dto);

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Long id);
}