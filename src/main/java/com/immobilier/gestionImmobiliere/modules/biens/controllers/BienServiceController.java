package com.immobilier.gestionImmobiliere.modules.biens.controllers;

import com.immobilier.gestionImmobiliere.modules.biens.apis.BienServiceAPI;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateBienServiceDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateBienServiceDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateDisponibiliteBienServiceDTO;
import com.immobilier.gestionImmobiliere.modules.biens.services.BienServiceService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BienServiceController implements BienServiceAPI {

    private final BienServiceService bienServiceService;

    public BienServiceController(BienServiceService bienServiceService) {
        this.bienServiceService = bienServiceService;
    }

    @Override
    public ResponseEntity<?> create(CreateBienServiceDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return bienServiceService.create(dto, currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> update(Integer id, UpdateBienServiceDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return bienServiceService.update(id, dto, currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> updateDisponibilite(Integer id, UpdateDisponibiliteBienServiceDTO dto) {
        return bienServiceService.updateDisponibilite(id, dto);
    }

    @Override
    public ResponseEntity<?> delete(Integer id) {
        return bienServiceService.delete(id);
    }
}