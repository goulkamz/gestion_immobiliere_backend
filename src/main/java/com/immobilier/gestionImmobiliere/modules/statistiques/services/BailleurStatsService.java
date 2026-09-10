package com.immobilier.gestionImmobiliere.modules.statistiques.services;

import com.immobilier.gestionImmobiliere.donnees.biens.repository.CourRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.BailleurMandatDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.BailleurPatrimoineDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.BailleurRevenuDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class BailleurStatsService {

    private final MaisonRepository maisonRepository;
    private final CourRepository courRepository;
    private final EcheanceLoyerRepository echeanceLoyerRepository;
    private final ContratMandatRepository contratMandatRepository;

    public BailleurStatsService(MaisonRepository maisonRepository, CourRepository courRepository,
                                EcheanceLoyerRepository echeanceLoyerRepository, ContratMandatRepository contratMandatRepository) {
        this.maisonRepository = maisonRepository;
        this.courRepository = courRepository;
        this.echeanceLoyerRepository = echeanceLoyerRepository;
        this.contratMandatRepository = contratMandatRepository;
    }

    public ResponseEntity<?> getPatrimoine(Integer idBailleur) {
        Map<String, Long> parStatut = new HashMap<>();
        long totalMaisons = 0;
        for (Object[] row : maisonRepository.countByStatutPourBailleur(idBailleur)) {
            String statut = String.valueOf(row[0]);
            long nb = ((Number) row[1]).longValue();
            parStatut.put(statut, nb);
            totalMaisons += nb;
        }

        BailleurPatrimoineDTO result = BailleurPatrimoineDTO.builder()
                .nbCours(courRepository.countByProprietaire_IdUserAndIsDeletedFalse(idBailleur))
                .nbMaisons(totalMaisons)
                .maisonsParStatut(parStatut)
                .tauxOccupation(maisonRepository.tauxOccupationPourBailleur(idBailleur))
                .build();

        return buildSuccessResponse(HttpStatus.OK, "Votre patrimoine", "BAILLEUR_PATRIMOINE", result);
    }

    public ResponseEntity<?> getRevenus(Integer idBailleur) {
        List<BailleurRevenuDTO> result = echeanceLoyerRepository.revenusPourBailleur(idBailleur).stream()
                .map(row -> BailleurRevenuDTO.builder()
                        .periodeMois(((java.sql.Date) row[0]).toLocalDate())
                        .montantDu(((Number) row[1]).doubleValue())
                        .montantRecu(((Number) row[2]).doubleValue())
                        .statut(String.valueOf(row[3]))
                        .build())
                .toList();

        return buildSuccessResponse(HttpStatus.OK, "Vos revenus locatifs", "BAILLEUR_REVENUS", result);
    }

    public ResponseEntity<?> getMandatActif(Integer idBailleur) {
        ContratMandat mandat = contratMandatRepository.findMandatActifPourBailleur(idBailleur)
                .orElseThrow(() -> new ResourceNotFoundException("mandat actif", idBailleur));

        BailleurMandatDTO result = BailleurMandatDTO.builder()
                .idMandat(mandat.getIdMandat())
                .referenceCour(mandat.getCour().getReferenceCour())
                .statut(mandat.getStatut().name())
                .dateDebut(LocalDate.from(mandat.getDateDebut()))
                .dateFin(mandat.getDateFin() != null ? LocalDate.from(mandat.getDateFin()) : null)
                .commissionPourcentage(mandat.getCommission())
                .nomAgent(mandat.getAgent().getNom() + " " + mandat.getAgent().getPrenom())
                .build();

        return buildSuccessResponse(HttpStatus.OK, "Votre mandat actif", "BAILLEUR_MANDAT", result);
    }
}