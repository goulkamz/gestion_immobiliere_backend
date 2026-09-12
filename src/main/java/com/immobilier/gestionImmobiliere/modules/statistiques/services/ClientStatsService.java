package com.immobilier.gestionImmobiliere.modules.statistiques.services;

import com.immobilier.gestionImmobiliere.donnees.biens.repository.LocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEntiteRemboursement;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementLocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.RemboursementRepository;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.ClientEcheanceDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.ClientLocationDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.ClientRemboursementDTO;
import com.immobilier.gestionImmobiliere.utils.DateUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class ClientStatsService {

    private final LocationBienServiceRepository locationBienServiceRepository;
    private final PaiementLocationBienServiceRepository paiementLocationRepository;
    private final RemboursementRepository remboursementRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final EcheanceLoyerRepository echeanceLoyerRepository;

    public ClientStatsService(LocationBienServiceRepository locationBienServiceRepository,
                              PaiementLocationBienServiceRepository paiementLocationRepository,
                              RemboursementRepository remboursementRepository,
                              ContratLocationRepository contratLocationRepository,
                              EcheanceLoyerRepository echeanceLoyerRepository) {
        this.locationBienServiceRepository = locationBienServiceRepository;
        this.paiementLocationRepository = paiementLocationRepository;
        this.remboursementRepository = remboursementRepository;
        this.contratLocationRepository = contratLocationRepository;
        this.echeanceLoyerRepository = echeanceLoyerRepository;
    }

    public ResponseEntity<?> getMesLocations(Integer idClient) {
        List<ClientLocationDTO> result = locationBienServiceRepository.findByClient_IdUserOrderByDateDebutDesc(idClient).stream()
                .map(l -> {
                    BigDecimal totalEncaisse = paiementLocationRepository.sumMontantByLocation(l.getIdLocationBienService());
                    BigDecimal totalRembourse = remboursementRepository.sumMontantByEntite(
                            TypeEntiteRemboursement.LOCATION_BIEN_SERVICE, l.getIdLocationBienService());
                    BigDecimal solde = l.getMontantTotal().subtract(totalEncaisse).add(totalRembourse);

                    return ClientLocationDTO.builder()
                            .idLocation(l.getIdLocationBienService())
                            .libelleBien(l.getBienService().getLibelle())
                            .dateDebut(l.getDateDebut())
                            .dateFin(l.getDateFin())
                            .montantTotal(l.getMontantTotal())
                            .totalEncaisse(totalEncaisse)
                            .solde(solde)
                            .statut(l.getStatut())
                            .build();
                })
                .toList();

        return buildSuccessResponse(HttpStatus.OK, "Vos locations", "CLIENT_LOCATIONS", result);
    }

    public ResponseEntity<?> getMesEcheances(Integer idClient) {
        List<Integer> idsContrats = contratLocationRepository.findByLocataire_IdUser(idClient).stream()
                .map(ContratLocation::getIdContratLocation).toList();

        List<ClientEcheanceDTO> result = (idsContrats.isEmpty()
                ? List.<EcheanceLoyer>of()
                : echeanceLoyerRepository.findEcheancesPourLocataire(idsContrats))
                .stream()
                .map(e -> ClientEcheanceDTO.builder()
                        .idEcheance(e.getIdEcheance())
                        .moisLibelle(DateUtils.nomMoisFrancais(e.getDateEcheance()) + " " + e.getDateEcheance().getYear())
                        .dateEcheance(e.getDateEcheance())
                        .montantDu(e.getMontantDu())
                        .montantPaye(e.getMontantPaye())
                        .statut(e.getStatut())
                        .build())
                .toList();

        return buildSuccessResponse(HttpStatus.OK, "Vos échéances de loyer", "CLIENT_ECHEANCES", result);
    }

    public ResponseEntity<?> getMesRemboursements(Integer idClient) {
        List<ClientRemboursementDTO> result = remboursementRepository.findRemboursementsPourClient(idClient).stream()
                .map(r -> ClientRemboursementDTO.builder()
                        .idRemboursement(r.getIdRemboursement())
                        .montant(r.getPaiement().getMontantPaiement())
                        .motif(r.getMotif())
                        .dateRemboursement(r.getPaiement().getDatePaiement())
                        .build())
                .toList();

        return buildSuccessResponse(HttpStatus.OK, "Vos remboursements", "CLIENT_REMBOURSEMENTS", result);
    }
}