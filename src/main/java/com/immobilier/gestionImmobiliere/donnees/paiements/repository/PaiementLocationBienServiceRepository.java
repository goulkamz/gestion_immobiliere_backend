package com.immobilier.gestionImmobiliere.donnees.paiements.repository;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.PaiementLocationBienService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaiementLocationBienServiceRepository extends JpaRepository<PaiementLocationBienService, PaiementLocationBienService.PaiementLocationId> {

    List<PaiementLocationBienService> findByIdLocationBienService(Integer idLocation);
}