package com.immobilier.gestionImmobiliere.modules.statistiques.services;

import com.immobilier.gestionImmobiliere.donnees.biens.repository.CourRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDepense;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDevis;
import com.immobilier.gestionImmobiliere.donnees.depenses.repository.DepenseBienRepository;
import com.immobilier.gestionImmobiliere.donnees.depenses.repository.DevisTravauxRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.BailleurDepensesDTO;
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
    private final DepenseBienRepository depenseRepository;
    private final DevisTravauxRepository devisRepository;

    public BailleurStatsService(MaisonRepository maisonRepository, CourRepository courRepository,
                                EcheanceLoyerRepository echeanceLoyerRepository, DepenseBienRepository depenseRepository,
                                DevisTravauxRepository devisRepository) {
        this.depenseRepository = depenseRepository;
        this.devisRepository = devisRepository;
        this.maisonRepository = maisonRepository;
        this.courRepository = courRepository;
        this.echeanceLoyerRepository = echeanceLoyerRepository;
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
                        .periodeMois((LocalDate) row[0])
                        .montantDu(((Number) row[1]).doubleValue())
                        .montantRecu(((Number) row[2]).doubleValue())
                        .statut(String.valueOf(row[3]))
                        .build())
                .toList();

        return buildSuccessResponse(HttpStatus.OK, "Vos revenus locatifs", "BAILLEUR_REVENUS", result);
    }

    public ResponseEntity<?> getDepenses(Integer idBailleur) {
        BailleurDepensesDTO result = BailleurDepensesDTO.builder()
                .resteARetenir(depenseRepository.sumRestantPourBailleur(idBailleur, StatutDepense.EN_COURS))
                .nombreEnCours(depenseRepository.countPourBailleur(idBailleur, StatutDepense.EN_COURS))
                .duDirectement(depenseRepository.sumRestantPourBailleur(idBailleur, StatutDepense.A_REGLER))
                .nombreDues(depenseRepository.countPourBailleur(idBailleur, StatutDepense.A_REGLER))
                .devisEnAttente(devisRepository.countByCour_Proprietaire_IdUserAndStatut(idBailleur, StatutDevis.EN_ATTENTE))
                .build();
        return buildSuccessResponse(HttpStatus.OK, "Vos dépenses et devis", "BAILLEUR_DEPENSES", result);
    }
}
