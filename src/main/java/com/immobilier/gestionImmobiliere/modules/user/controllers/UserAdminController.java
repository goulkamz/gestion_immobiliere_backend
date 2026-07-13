package com.immobilier.gestionImmobiliere.modules.user.controllers;

import com.immobilier.gestionImmobiliere.donnees.user.model.ERole;
import com.immobilier.gestionImmobiliere.modules.user.apis.UserAdminAPI;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.*;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import com.immobilier.gestionImmobiliere.modules.user.services.UserAdminService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController implements UserAdminAPI {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @Override
    public ResponseEntity<?> getAll(ERole role, Pageable pageable) {
        return userAdminService.getAll(role, pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id) {
        return userAdminService.getById(id);
    }

    @Override
    public ResponseEntity<?> create(CreateUserByAdminDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return userAdminService.create(dto, currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> update(Integer id, UpdateUserByAdminDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return userAdminService.update(id, dto, currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> updateRole(Integer id, UpdateUserRoleDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return userAdminService.updateRole(id, dto, currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> updateStatus(Integer id, UpdateUserStatusDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return userAdminService.updateStatus(id, dto, currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> delete(Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return userAdminService.delete(id, currentUser.getIdUser());
    }
}