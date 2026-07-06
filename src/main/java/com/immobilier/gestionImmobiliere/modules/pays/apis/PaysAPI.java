package com.immobilier.gestionImmobiliere.modules.pays.apis;

import com.immobilier.gestionImmobiliere.modules.pays.dto.requests.CreatePaysDTO;
import com.immobilier.gestionImmobiliere.modules.pays.dto.requests.UpdatePaysDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/pays")
public interface PaysAPI {

    @GetMapping
    ResponseEntity<?> getAll(Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Long id);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreatePaysDTO dto);

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdatePaysDTO dto);

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Long id);
}