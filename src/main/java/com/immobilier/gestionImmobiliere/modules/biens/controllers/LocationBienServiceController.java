package com.immobilier.gestionImmobiliere.modules.biens.controllers;

import com.immobilier.gestionImmobiliere.modules.biens.apis.LocationBienServiceAPI;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.*;
import com.immobilier.gestionImmobiliere.modules.biens.services.LocationBienServiceService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocationBienServiceController implements LocationBienServiceAPI {

    private final LocationBienServiceService locationService;

    public LocationBienServiceController(LocationBienServiceService locationService) {
        this.locationService = locationService;
    }

    @Override
    public ResponseEntity<?> getAll(Pageable pageable, UserDetailsImpl currentUser) {
        return locationService.getAll(pageable, currentUser);
    }

    @Override
    public ResponseEntity<?> getById(Integer id, UserDetailsImpl currentUser) {
        return locationService.getById(id, currentUser);
    }

    @Override
    public ResponseEntity<?> create(CreateLocationBienServiceDTO dto, UserDetailsImpl currentUser) {
        return locationService.create(dto, currentUser.getIdUser());
    }

    /**
     * @param id
     * @param dto
     * @param currentUser
     * @return
     */
    @Override
    public ResponseEntity<?> confirmer(Integer id, ConfirmerLocationDTO dto, UserDetailsImpl currentUser) {
        return locationService.confirmer(id,dto,currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutLocationBienServiceDTO dto) {
        return locationService.updateStatut(id, dto);
    }

    @Override
    public ResponseEntity<?> modifierDuree(Integer id, ModifierDureeLocationDTO dto, UserDetailsImpl currentUser) {
        return locationService.modifierDuree(id, dto, currentUser.getIdUser());
    }

    /**
     * @param id
     * @param currentUser
     * @return
     */
    @Override
    public ResponseEntity<?> annuler(Integer id, UserDetailsImpl currentUser) {
        return locationService.annuler(id,currentUser);
    }

    /**
     * @param id
     * @param dto
     * @param currentUser
     * @return
     */
    @Override
    public ResponseEntity<?> rembourser(Integer id, CreerRemboursementDTO dto, UserDetailsImpl currentUser) {
        return locationService.rembourser(id,dto,currentUser.getIdUser());
    }

    /**
     * @param id
     * @return
     */
    @Override
    public ResponseEntity<?> getRemboursements(Integer id) {
        return locationService.getRemboursements(id);
    }
}