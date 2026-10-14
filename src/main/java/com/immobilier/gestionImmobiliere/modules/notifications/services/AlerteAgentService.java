package com.immobilier.gestionImmobiliere.modules.notifications.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Contact;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.Offre;
import com.immobilier.gestionImmobiliere.donnees.biens.model.LocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.LocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.notifications.model.TypeNotification;
import com.immobilier.gestionImmobiliere.donnees.reservations.model.ReservationMaison;
import com.immobilier.gestionImmobiliere.donnees.user.model.ERole;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Alertes in-app destinees aux agents et admins (tranche 2) : une notification par destinataire actif, jamais de destinataire venu de la requete
@Service
public class AlerteAgentService {

    private static final Logger log = LoggerFactory.getLogger(AlerteAgentService.class);
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // Meme seuil que le filtre « A echeance » de la liste des locations
    private static final int JOURS_PREAVIS_ECHEANCE = 2;

    private static final String LIEN_LOCATIONS = "/espace-agent/locations-biens-services";
    private static final String LIEN_RESERVATIONS = "/espace-agent/reservations";
    private static final String LIEN_OFFRES = "/espace-agent/offres";
    private static final String LIEN_CONTACTS = "/espace-agent/contacts";

    private final NotificationInAppService notificationService;
    private final UserRepository userRepository;
    private final LocationBienServiceRepository locationRepository;

    public AlerteAgentService(NotificationInAppService notificationService, UserRepository userRepository,
                              LocationBienServiceRepository locationRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
    }

    // Une alerte ne doit jamais faire echouer l'action metier qui la declenche
    public void nouvelleDemandeLocation(LocationBienService location) {
        try {
            String titre = "Nouvelle demande de location";
            String message = nomComplet(location.getClient()) + " demande " + location.getBienService().getLibelle()
                    + " du " + location.getDateDebut().format(FORMAT_DATE) + " au " + location.getDateFin().format(FORMAT_DATE) + ".";
            String cle = "LOCATION_DEMANDE:" + location.getIdLocationBienService();
            notifierAgents(TypeNotification.DEMANDE, titre, message, LIEN_LOCATIONS, cle);
        } catch (RuntimeException e) {
            log.warn("Alerte de nouvelle demande de location non creee : {}", e.getMessage());
        }
    }

    public void nouvelleReservation(ReservationMaison reservation) {
        try {
            String titre = "Nouvelle réservation de maison";
            String message = nomComplet(reservation.getUser()) + " a réservé " + reservation.getMaison().getNomCommunMaison()
                    + " à partir du " + reservation.getDateDebut().format(FORMAT_DATE) + ".";
            String cle = "RESERVATION_DEMANDE:" + reservation.getIdReservation();
            notifierAgents(TypeNotification.DEMANDE, titre, message, LIEN_RESERVATIONS, cle);
        } catch (RuntimeException e) {
            log.warn("Alerte de nouvelle reservation non creee : {}", e.getMessage());
        }
    }

    public void nouvelleOffre(Offre offre) {
        try {
            String message = offre.getNomComplet() + " propose " + (vide(offre.getTitre()) ? "un bien" : "« " + offre.getTitre() + " »")
                    + (vide(offre.getTypeOffre()) ? "" : " (" + offre.getTypeOffre() + ")") + ".";
            notifierAgents(TypeNotification.DEMANDE, "Nouvelle offre déposée", message, LIEN_OFFRES, "OFFRE_DEPOT:" + offre.getIdOffre());
        } catch (RuntimeException e) {
            log.warn("Alerte de nouvelle offre non creee : {}", e.getMessage());
        }
    }

    public void nouveauContact(Contact contact) {
        try {
            String message = contact.getNomComplet() + " vous a écrit" + (vide(contact.getSujet()) ? "." : " : " + contact.getSujet() + ".");
            notifierAgents(TypeNotification.INFO, "Nouveau message de contact", message, LIEN_CONTACTS, "CONTACT_MESSAGE:" + contact.getIdContact());
        } catch (RuntimeException e) {
            log.warn("Alerte de nouveau contact non creee : {}", e.getMessage());
        }
    }

    /**
     * Job quotidien : alerte les agents pour chaque location ACTIF qui se termine dans les
     * JOURS_PREAVIS_ECHEANCE jours ou dont la fin est depassee. La cle contient la date de fin :
     * une prolongation provoque donc une nouvelle alerte a la nouvelle echeance, sans doublon sinon.
     */
    @Transactional
    public void alerterLocationsAEcheance() {
        LocalDate aujourdhui = LocalDate.now();
        List<LocationBienService> locations = locationRepository.findByStatutAndDateFinBefore(
                StatutLocationBienService.ACTIF, aujourdhui.plusDays(JOURS_PREAVIS_ECHEANCE + 1L).atStartOfDay());
        int creees = 0;
        for (LocationBienService l : locations) {
            LocalDate fin = l.getDateFin().toLocalDate();
            boolean depassee = fin.isBefore(aujourdhui);
            String quand = depassee ? "est terminée depuis le " : "se termine le ";
            String message = l.getBienService().getLibelle() + " (" + nomComplet(l.getClient()) + ") " + quand
                    + fin.format(FORMAT_DATE) + ". Clôturez ou prolongez la location.";
            String cle = (depassee ? "LOCATION_RETARD:" : "LOCATION_ECHEANCE:") + l.getIdLocationBienService() + ":" + fin;
            creees += notifierAgents(TypeNotification.ECHEANCE,
                    depassee ? "Location à clôturer" : "Location bientôt terminée", message, LIEN_LOCATIONS, cle);
        }
        if (creees > 0) log.info("Alertes de fin de location : {} notification(s) creee(s)", creees);
    }

    // Une ligne par agent ou admin actif ; retourne le nombre de notifications effectivement creees
    private int notifierAgents(TypeNotification type, String titre, String message, String lien, String cleAlerte) {
        int creees = 0;
        for (User agent : userRepository.findByRole_LibelleRoleInAndIsDeletedFalse(List.of(ERole.ROLE_AGENT, ERole.ROLE_ADMIN))) {
            if (notificationService.creer(agent.getIdUser(), type, titre, message, lien, cleAlerte)) creees++;
        }
        return creees;
    }

    private boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private String nomComplet(User u) {
        return (u.getPrenom() + " " + u.getNom()).trim();
    }
}
