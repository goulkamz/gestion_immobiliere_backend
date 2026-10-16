package com.immobilier.gestionImmobiliere.modules.notifications.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Contact;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.Demande;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.Offre;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.Temoignage;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.utils.DateUtils;
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
    private static final String LIEN_DEMANDES = "/espace-agent/demandes";
    private static final String LIEN_TEMOIGNAGES = "/espace-agent/temoignages";
    private static final String LIEN_UTILISATEURS = "/espace-agent/utilisateurs";
    private static final String LIEN_REVERSEMENTS = "/espace-agent/reversements";
    private static final String LIEN_OFFRES = "/espace-agent/offres";
    private static final String LIEN_CONTACTS = "/espace-agent/contacts";

    private final NotificationInAppService notificationService;
    private final UserRepository userRepository;
    private final LocationBienServiceRepository locationRepository;
    private final ContratMandatRepository mandatRepository;

    public AlerteAgentService(NotificationInAppService notificationService, UserRepository userRepository,
                              LocationBienServiceRepository locationRepository, ContratMandatRepository mandatRepository) {
        this.mandatRepository = mandatRepository;
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
            notifierAgents(TypeNotification.DEMANDE, titre, message, cible(LIEN_LOCATIONS, location.getIdLocationBienService()), cle);
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
            notifierAgents(TypeNotification.DEMANDE, titre, message, cible(LIEN_RESERVATIONS, reservation.getIdReservation()), cle);
        } catch (RuntimeException e) {
            log.warn("Alerte de nouvelle reservation non creee : {}", e.getMessage());
        }
    }

    public void nouvelleDemande(Demande demande) {
        try {
            String message = demande.getNomComplet() + " recherche " + (vide(demande.getTypeBien()) ? "un bien" : demande.getTypeBien())
                    + (demande.getVille() != null ? " à " + demande.getVille().getNomVille() : vide(demande.getVilleAutre()) ? "" : " à " + demande.getVilleAutre()) + ".";
            notifierAgents(TypeNotification.DEMANDE, "Nouvelle demande de bien", message, cible(LIEN_DEMANDES, demande.getIdDemande()), "DEMANDE_DEPOT:" + demande.getIdDemande());
        } catch (RuntimeException e) {
            log.warn("Alerte de nouvelle demande non creee : {}", e.getMessage());
        }
    }

    public void nouveauTemoignage(Temoignage t) {
        try {
            notifierAgents(TypeNotification.INFO, "Nouveau témoignage à valider", t.getNomAuteur() + " a déposé un témoignage.",
                    cible(LIEN_TEMOIGNAGES, t.getIdTemoignage()), "TEMOIGNAGE_DEPOT:" + t.getIdTemoignage());
        } catch (RuntimeException e) {
            log.warn("Alerte de nouveau temoignage non creee : {}", e.getMessage());
        }
    }

    public void nouveauCompte(User u) {
        try {
            notifierAgents(TypeNotification.INFO, "Nouveau compte client", nomComplet(u) + " vient d'activer son compte client.",
                    cible(LIEN_UTILISATEURS, u.getIdUser()), "COMPTE_CREE:" + u.getIdUser());
        } catch (RuntimeException e) {
            log.warn("Alerte de nouveau compte non creee : {}", e.getMessage());
        }
    }

    // Reversement au bailleur non confirme dans le delai de tolerance (echeance MANDAT passee EN_RETARD)
    public void reversementEnRetard(EcheanceLoyer e) {
        try {
            String mois = DateUtils.nomMoisFrancais(e.getDateEcheance()) + " " + e.getDateEcheance().getYear();
            String message = mandatRepository.findById(e.getEntiteEcheanceId())
                    .map(m -> "Le reversement de " + mois + " pour la cour " + m.getCour().getReferenceCour()
                            + (m.getCour().getProprietaire() != null ? " (" + nomComplet(m.getCour().getProprietaire()) + ")" : "") + " est en retard.")
                    .orElse("Un reversement de " + mois + " est en retard.");
            notifierAgents(TypeNotification.ECHEANCE, "Reversement en retard", message,
                    cible(LIEN_REVERSEMENTS, e.getIdEcheance()), "MANDAT_RETARD:" + e.getIdEcheance());
        } catch (RuntimeException ex) {
            log.warn("Alerte de reversement en retard non creee : {}", ex.getMessage());
        }
    }

    public void nouvelleOffre(Offre offre) {
        try {
            String message = offre.getNomComplet() + " propose " + (vide(offre.getTitre()) ? "un bien" : "« " + offre.getTitre() + " »")
                    + (vide(offre.getTypeOffre()) ? "" : " (" + offre.getTypeOffre() + ")") + ".";
            notifierAgents(TypeNotification.DEMANDE, "Nouvelle offre déposée", message, cible(LIEN_OFFRES, offre.getIdOffre()), "OFFRE_DEPOT:" + offre.getIdOffre());
        } catch (RuntimeException e) {
            log.warn("Alerte de nouvelle offre non creee : {}", e.getMessage());
        }
    }

    public void nouveauContact(Contact contact) {
        try {
            String message = contact.getNomComplet() + " vous a écrit" + (vide(contact.getSujet()) ? "." : " : " + contact.getSujet() + ".");
            notifierAgents(TypeNotification.INFO, "Nouveau message de contact", message, cible(LIEN_CONTACTS, contact.getIdContact()), "CONTACT_MESSAGE:" + contact.getIdContact());
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
                    depassee ? "Location à clôturer" : "Location bientôt terminée", message, cible(LIEN_LOCATIONS, l.getIdLocationBienService()), cle);
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

    // Lien vers la page, avec l'identifiant de la ligne concernee (la page la repere et la met en evidence)
    private static String cible(String lien, Integer id) {
        return id == null ? lien : lien + "?cible=" + id;
    }

    private boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private String nomComplet(User u) {
        return (u.getPrenom() + " " + u.getNom()).trim();
    }
}
