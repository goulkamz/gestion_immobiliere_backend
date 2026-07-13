package com.immobilier.gestionImmobiliere.modules.biens.apis;

import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateCourDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateCourDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/cours")
public interface CourAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idSecteur, Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateCourDTO dto);

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateCourDTO dto);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateCourDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateCourDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);


    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}