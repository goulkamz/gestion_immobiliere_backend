package com.immobilier.gestionImmobiliere.modules.statistiques.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.LocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.reservations.model.StatutReservation;
import com.immobilier.gestionImmobiliere.donnees.reservations.repository.ReservationMaisonRepository;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.DemandeEnAttenteDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.EcheanceRetardAgentDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.ReservationEnAttenteDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class AgentStatsService {

    private final LocationBienServiceRepository locationBienServiceRepository;
    private final ReservationMaisonRepository reservationMaisonRepository;
    private final ContratMandatRepository contratMandatRepository;
    private final EcheanceLoyerRepository echeanceLoyerRepository;

    public AgentStatsService(LocationBienServiceRepository locationBienServiceRepository,
                             ReservationMaisonRepository reservationMaisonRepository,
                             ContratMandatRepository contratMandatRepository,
                             EcheanceLoyerRepository echeanceLoyerRepository) {
        this.locationBienServiceRepository = locationBienServiceRepository;
        this.reservationMaisonRepository = reservationMaisonRepository;
        this.contratMandatRepository = contratMandatRepository;
        this.echeanceLoyerRepository = echeanceLoyerRepository;
    }

    public ResponseEntity<?> getDemandesEnAttente() {
        List<DemandeEnAttenteDTO> result = locationBienServiceRepository
                .findByStatutOrderByDateDebutAsc(StatutLocationBienService.EN_ATTENTE).stream()
                .map(l -> DemandeEnAttenteDTO.builder()
                        .idLocation(l.getIdLocationBienService())
                        .libelleBien(l.getBienService().getLibelle())
                        .nomClient(l.getClient().getNom() + " " + l.getClient().getPrenom())
                        .dateDebut(l.getDateDebut())
                        .dateFin(l.getDateFin())
                        .montantEstime(l.getMontantTotal())
                        .statut(l.getStatut())
                        .build())
                .toList();

        return buildSuccessResponse(HttpStatus.OK, "Demandes en attente de confirmation", "DEMANDES_EN_ATTENTE", result);
    }

    public ResponseEntity<?> getReservationsEnAttente() {
        List<ReservationEnAttenteDTO> result = reservationMaisonRepository
                .findByStatutOrderByDateDebutAsc(StatutReservation.EN_ATTENTE).stream()
                .map(r -> ReservationEnAttenteDTO.builder()
                        .idReservation(r.getIdReservation())
                        .nomMaison(r.getMaison().getNomCommunMaison())
                        .nomClient(r.getUser().getNom() + " " + r.getUser().getPrenom())
                        .dateDebut(r.getDateDebut())
                        .dateFin(r.getDateFin())
                        .build())
                .toList();

        return buildSuccessResponse(HttpStatus.OK, "Réservations en attente", "RESERVATIONS_EN_ATTENTE", result);
    }

    public ResponseEntity<?> getMesMandats(Integer idAgent) {
        Map<String, Long> parStatut = contratMandatRepository.findByAgent_IdUser(idAgent).stream()
                .collect(Collectors.groupingBy(m -> m.getStatut().name(), Collectors.counting()));

        return buildSuccessResponse(HttpStatus.OK, "Mes mandats par statut", "MES_MANDATS", parStatut);
    }

    public ResponseEntity<?> getEcheancesEnRetard(Integer idAgent) {
        List<EcheanceRetardAgentDTO> result = echeanceLoyerRepository.echeancesEnRetardPourAgent(idAgent).stream()
                .map(row -> EcheanceRetardAgentDTO.builder()
                        .idEcheance((Integer) row[0])
                        .type((String) row[1])
                        .libelleContrat((String) row[2])
                        .montantDu(((Number) row[3]).doubleValue())
                        .dateEcheance(row[4].toString())
                        .build())
                .toList();

        return buildSuccessResponse(HttpStatus.OK, "Échéances en retard sur vos contrats", "ECHEANCES_RETARD_AGENT", result);
    }
}