package com.immobilier.gestionImmobiliere.modules.statistiques.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Annonce;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutAnnonce;
import com.immobilier.gestionImmobiliere.donnees.annonces.repository.AnnonceRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.BienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.CourRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.LocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.localisation.repository.SecteurRepository;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.PublicStatsDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class PublicStatsService {

    private final MaisonRepository maisonRepository;
    private final BienServiceRepository bienServiceRepository;
    private final SecteurRepository secteurRepository;
    private final CourRepository courRepository;
    private final LocationBienServiceRepository locationBienServiceRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final AnnonceRepository annonceRepository;

    public PublicStatsService(MaisonRepository maisonRepository, BienServiceRepository bienServiceRepository, SecteurRepository secteurRepository, CourRepository courRepository, LocationBienServiceRepository locationBienServiceRepository, ContratLocationRepository contratLocationRepository, AnnonceRepository annonceRepository) {
        this.maisonRepository = maisonRepository;
        this.bienServiceRepository = bienServiceRepository;
        this.secteurRepository = secteurRepository;
        this.courRepository = courRepository;
        this.locationBienServiceRepository = locationBienServiceRepository;
        this.contratLocationRepository = contratLocationRepository;
        this.annonceRepository = annonceRepository;
    }



    public ResponseEntity<?> getStats() {
        long maisonsDisponibles = maisonRepository.countByIsDeletedFalseAndStatut(StatutMaison.DISPONIBLE);
        long biensDisponibles = bienServiceRepository.countByIsDeletedFalseAndDisponibilite(StatutBienService.DISPONIBLE);

        long locationsRealisees = locationBienServiceRepository.countByIsDeletedFalseAndStatutIn(
                List.of(StatutLocationBienService.ACTIF, StatutLocationBienService.TERMINE))
                + contratLocationRepository.countByIsDeletedFalseAndStatutIn(
                List.of(StatutLocation.ACTIF, StatutLocation.TERMINE));

        PublicStatsDTO result = PublicStatsDTO.builder()
                .maisonsDisponibles(maisonsDisponibles)
                .biensDisponibles(biensDisponibles)
                .villesCouvertes(secteurRepository.countVillesCouvertes())
                .secteursCouverts(secteurRepository.countByIsDeletedFalse())
                .proprietesGerees(courRepository.count())
                .locationsRealisees(locationsRealisees)
                .build();

        return buildSuccessResponse(HttpStatus.OK, "Statistiques publiques", "PUBLIC_STATS", result);
    }

    public ResponseEntity<?> getDernieresAnnonces() {
        List<Annonce> annonces = annonceRepository.findTop5ByIsDeletedFalseAndStatutOrderByDatePublicationDesc(StatutAnnonce.ACTIVE);
        // mapping vers un DTO annonce simple, sans donnée sensible
        return buildSuccessResponse(HttpStatus.OK, "Dernières annonces", "PUBLIC_ANNONCES", annonces);
    }
}