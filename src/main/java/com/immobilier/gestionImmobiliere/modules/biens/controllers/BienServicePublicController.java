package com.immobilier.gestionImmobiliere.modules.biens.controllers;

import com.immobilier.gestionImmobiliere.modules.biens.apis.BienServicePublicAPI;
import com.immobilier.gestionImmobiliere.modules.biens.services.BienServiceService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutBienService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BienServicePublicController implements BienServicePublicAPI {
    private final BienServiceService bienServiceService;

    public BienServicePublicController(BienServiceService bienServiceService) {
        this.bienServiceService = bienServiceService;
    }

    /**
     * @param idSecteur
     * @param idCategorie
     * @param recherche
     * @param pageable
     * @return
     */
    @Override
    public ResponseEntity<?> getAll(Integer idSecteur, Integer idCategorie, String recherche, StatutBienService disponibilite, Pageable pageable) {
        return bienServiceService.getAll(idSecteur, idCategorie, recherche, disponibilite, pageable);
    }



    /**
     * @param id
     * @return
     */
    @Override
    public ResponseEntity<?> getById(Integer id) {
        return bienServiceService.getById(id);
    }
}
