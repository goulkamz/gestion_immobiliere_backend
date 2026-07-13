package com.immobilier.gestionImmobiliere.modules.user.apis;

import com.immobilier.gestionImmobiliere.donnees.user.model.ERole;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.*;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/admin/users")
public interface UserAdminAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) ERole role, Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateUserByAdminDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateUserByAdminDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/role")
    ResponseEntity<?> updateRole(@PathVariable Integer id, @Valid @RequestBody UpdateUserRoleDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/status")
    ResponseEntity<?> updateStatus(@PathVariable Integer id, @Valid @RequestBody UpdateUserStatusDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);
}