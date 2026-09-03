package com.immobilier.gestionImmobiliere.modules.statistiques.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutContact;
import com.immobilier.gestionImmobiliere.donnees.annonces.repository.*;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.*;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.*;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.*;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.BailleurTopDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.LocataireCreanceDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.VilleStatsDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.AdminStatsDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class AdminStatsService {

    private final UserRepository userRepository;
    private final CourRepository courRepository;
    private final MaisonRepository maisonRepository;
    private final BienServiceRepository bienServiceRepository;
    private final ContratMandatRepository contratMandatRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final EcheanceLoyerRepository echeanceLoyerRepository;
    private final LocationBienServiceRepository locationBienServiceRepository;
    private final PaiementLocationBienServiceRepository paiementLocationRepository;
    private final RemboursementRepository remboursementRepository;
    private final AnnonceRepository annonceRepository;
    private final DemandeRepository demandeRepository;
    private final OffreRepository offreRepository;
    private final ContactRepository contactRepository;

    public AdminStatsService(UserRepository userRepository, CourRepository courRepository, MaisonRepository maisonRepository,
                             BienServiceRepository bienServiceRepository, ContratMandatRepository contratMandatRepository,
                             ContratLocationRepository contratLocationRepository, EcheanceLoyerRepository echeanceLoyerRepository,
                             LocationBienServiceRepository locationBienServiceRepository,
                             PaiementLocationBienServiceRepository paiementLocationRepository,
                             RemboursementRepository remboursementRepository, AnnonceRepository annonceRepository,
                             DemandeRepository demandeRepository, OffreRepository offreRepository,
                             ContactRepository contactRepository) {
        this.userRepository = userRepository;
        this.courRepository = courRepository;
        this.maisonRepository = maisonRepository;
        this.bienServiceRepository = bienServiceRepository;
        this.contratMandatRepository = contratMandatRepository;
        this.contratLocationRepository = contratLocationRepository;
        this.echeanceLoyerRepository = echeanceLoyerRepository;
        this.locationBienServiceRepository = locationBienServiceRepository;
        this.paiementLocationRepository = paiementLocationRepository;
        this.remboursementRepository = remboursementRepository;
        this.annonceRepository = annonceRepository;
        this.demandeRepository = demandeRepository;
        this.offreRepository = offreRepository;
        this.contactRepository = contactRepository;
    }

    public ResponseEntity<?> getStats() {
        Object[] sumEcheances = echeanceLoyerRepository.sumDuEtPaye();
        Object[] sumRetard = echeanceLoyerRepository.sumEnRetard();

        AdminStatsDTO stats = AdminStatsDTO.builder()
                .totalUtilisateurs(userRepository.count())
                .utilisateursParRole(toMap(userRepository.countByRole()))
                .inscriptionsCeMois(userRepository.countInscriptionsCeMois())

                .totalCours(courRepository.count())
                .maisonsParStatut(toMap(maisonRepository.countByStatut()))
                .tauxOccupationMaisons(maisonRepository.tauxOccupation())
                .biensServiceParDisponibilite(toMap(bienServiceRepository.countByCategorieEtDisponibilite()))

                .mandatsParStatut(toMap(contratMandatRepository.countByStatut()))
                .contratsLocationParStatut(toMap(contratLocationRepository.countByStatut()))
                .mandatsExpirantSous30Jours(contratMandatRepository.countExpirantSous30Jours())

                .montantDuTotal((Double) sumEcheances[0])
                .montantPayeTotal((Double) sumEcheances[1])
                .nombreEcheancesEnRetard((Long) sumRetard[0])
                .montantEcheancesEnRetard((Double) sumRetard[1])

                .locationsBienServiceParStatut(toMap(locationBienServiceRepository.countByStatut()))
                .totalEncaisseBienService(paiementLocationRepository.sumTotalEncaisse())
                .totalRembourseBienService(remboursementRepository.sumRembourseBienService())

                .annoncesParStatut(toMap(annonceRepository.countByStatut()))
                .demandesParStatut(toMap(demandeRepository.countByStatut()))
                .offresNonTraitees(offreRepository.countByIsDeletedFalse())
                .contactsNonLus(contactRepository.countByIsDeletedFalseAndStatut(StatutContact.NON_LU))

                .connexionsRecentes7j(userRepository.countConnexionsRecentes())
                .build();

        return buildSuccessResponse(HttpStatus.OK, "Statistiques administrateur", "ADMIN_STATS", stats);
    }

    private Map<String, Long> toMap(java.util.List<Object[]> rows) {
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return result;
    }


    public ResponseEntity<?> getStatsParVille() {
        List<Object[]> maisonsParVille = maisonRepository.countMaisonsParVille();
        List<Object[]> locatairesParVille = contratLocationRepository.countLocatairesActifsParVille();

        Map<String, Long> nbLocatairesMap = new HashMap<>();
        for (Object[] row : locatairesParVille) {
            nbLocatairesMap.put((String) row[0], ((Number) row[1]).longValue());
        }

        List<VilleStatsDTO> result = maisonsParVille.stream()
                .map(row -> {
                    String ville = (String) row[0];
                    return VilleStatsDTO.builder()
                            .nomVille(ville)
                            .nbMaisons(((Number) row[1]).longValue())
                            .nbLocatairesActifs(nbLocatairesMap.getOrDefault(ville, 0L))
                            .build();
                })
                .toList();

        return buildSuccessResponse(HttpStatus.OK, "Statistiques par ville", "STATS_PAR_VILLE", result);
    }

    public ResponseEntity<?> getTopBailleurs(int limite) {
        List<BailleurTopDTO> result = courRepository.topBailleursParNbMaisons(limite).stream()
                .map(row -> BailleurTopDTO.builder()
                        .idBailleur((Integer) row[0])
                        .nomComplet((String) row[1])
                        .nbMaisons(((Number) row[2]).longValue())
                        .build())
                .toList();

        return buildSuccessResponse(HttpStatus.OK, "Top bailleurs par nombre de maisons", "TOP_BAILLEURS", result);
    }

    public ResponseEntity<?> getLocatairesEnCreance() {
        List<LocataireCreanceDTO> result = echeanceLoyerRepository.locatairesEnCreance().stream()
                .map(row -> LocataireCreanceDTO.builder()
                        .idLocataire((Integer) row[0])
                        .nomComplet((String) row[1])
                        .nbEcheancesEnRetard(((Number) row[2]).longValue())
                        .montantDu(((Number) row[3]).doubleValue())
                        .build())
                .toList();

        return buildSuccessResponse(HttpStatus.OK, "Locataires en créance", "LOCATAIRES_EN_CREANCE", result);
    }

}