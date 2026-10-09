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
public class UserAdminController implements UserAdminAPI {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    // Lecture ouverte à l'agent : besoin de lister les bailleurs/gestionnaires
    // pour peupler les sélecteurs de création de cour/bien-service.
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> getAll(ERole role, String recherche, Pageable pageable) {
        return userAdminService.getAll(role, recherche, pageable);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> getById(Integer id) {
        return userAdminService.getById(id);
    }

    // Ouvert a l'agent, mais limite aux roles client/bailleur (controle dans le service)
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> create(CreateUserByAdminDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return userAdminService.create(dto, currentUser.getIdUser(), currentUser.hasRole("ADMIN"));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(Integer id, UpdateUserByAdminDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return userAdminService.update(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRole(Integer id, UpdateUserRoleDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return userAdminService.updateRole(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(Integer id, UpdateUserStatusDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return userAdminService.updateStatus(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return userAdminService.delete(id, currentUser.getIdUser());
    }
}