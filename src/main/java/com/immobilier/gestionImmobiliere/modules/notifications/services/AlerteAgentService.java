package com.immobilier.gestionImmobiliere.modules.notifications.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Contact;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.Demande;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.Offre;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.Temoignage;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseBien;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.DevisTravaux;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDepense;
import com.immobilier.gestionImmobiliere.donnees.depenses.repository.DepenseBienRepository;
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
    private static final String LIEN_DEVIS = "/espace-agent/devis";

    private static final String LIEN_MANDATS = "/espace-agent/mandats";
    // Un mandat qui se termine est signalé à l'agent dès ce nombre de jours, puis une seconde fois à l'approche de la fin
    private static final int JOURS_PREAVIS_MANDAT_LONG = 30;
    private static final int JOURS_PREAVIS_MANDAT_COURT = 7;

    private final NotificationInAppService notificationService;
    private final UserRepository userRepository;
    private final LocationBienServiceRepository locationRepository;
    private final ContratMandatRepository mandatRepository;
    private final DepenseBienRepository depenseRepository;

    public AlerteAgentService(NotificationInAppService notificationService, UserRepository userRepository,
                              LocationBienServiceRepository locationRepository, ContratMandatRepository mandatRepository,
                              DepenseBienRepository depenseRepository) {
        this.depenseRepository = depenseRepository;
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

    /**
     * Job quotidien : mandats ACTIF dont la date de fin approche ou est depassee. L'agent du mandat (ou tous les agents
     * s'il n'y en a pas) et les admins sont prevenus a 30 jours, a 7 jours, puis chaque semaine tant que le mandat n'est
     * ni resilie ni prolonge par avenant. Les cles portent la date de fin (une prolongation relance le cycle) et, apres
     * l'echeance, la semaine en cours : un rappel par semaine, jamais de doublon dans la meme semaine.
     */
    @Transactional
    public void alerterMandatsAEcheance() {
        LocalDate aujourdhui = LocalDate.now();
        int creees = 0;
        for (ContratMandat m : mandatRepository.findByStatut(StatutMandat.ACTIF)) {
            if (m.getDateFin() == null) continue;
            LocalDate fin = m.getDateFin().toLocalDate();
            long jours = java.time.temporal.ChronoUnit.DAYS.between(aujourdhui, fin);
            if (jours > JOURS_PREAVIS_MANDAT_LONG) continue;

            List<DepenseBien> enCours = depenseRepository.findByMandat_IdMandatAndStatutOrderByDateDepenseAscIdDepenseAsc(
                    m.getIdMandat(), StatutDepense.EN_COURS);
            java.math.BigDecimal reste = enCours.stream()
                    .map(d -> d.getMontant().subtract(d.getMontantImpute()).subtract(d.getMontantRegle()).subtract(d.getMontantAbandonne()))
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            String cour = m.getCour().getReferenceCour();
            String depenses = enCours.isEmpty() ? ""
                    : " " + enCours.size() + " dépense(s) sont en cours de retenue (" + reste.stripTrailingZeros().toPlainString()
                            + " restant à retenir) : résilier le mandat retient ce solde d'un coup sur les derniers reversements.";

            String titre;
            String message;
            String cle;
            if (jours < 0) {
                String semaine = aujourdhui.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR) + "-S" + aujourdhui.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                titre = "Mandat échu à régulariser";
                message = "Le mandat de la cour " + cour + " est arrivé à terme le " + fin.format(FORMAT_DATE)
                        + " et reste actif. Résiliez-le ou prolongez-le par avenant." + depenses;
                cle = "MANDAT_ECHU:" + m.getIdMandat() + ":" + fin + ":" + semaine;
            } else if (jours <= JOURS_PREAVIS_MANDAT_COURT) {
                titre = "Mandat bientôt terminé";
                message = "Le mandat de la cour " + cour + " se termine " + (jours == 0 ? "aujourd'hui" : "le " + fin.format(FORMAT_DATE))
                        + ". Prévoyez sa prolongation par avenant ou sa résiliation." + depenses;
                cle = "MANDAT_FIN_J" + JOURS_PREAVIS_MANDAT_COURT + ":" + m.getIdMandat() + ":" + fin;
            } else {
                titre = "Mandat à renouveler";
                message = "Le mandat de la cour " + cour + " se termine le " + fin.format(FORMAT_DATE) + " (dans " + jours
                        + " jours). Prévoyez sa prolongation par avenant ou sa résiliation." + depenses;
                cle = "MANDAT_FIN_J" + JOURS_PREAVIS_MANDAT_LONG + ":" + m.getIdMandat() + ":" + fin;
            }

            java.util.Map<Integer, User> destinataires = new java.util.LinkedHashMap<>();
            userRepository.findByRole_LibelleRoleInAndIsDeletedFalse(List.of(ERole.ROLE_ADMIN)).forEach(u -> destinataires.put(u.getIdUser(), u));
            if (m.getAgent() != null) {
                destinataires.put(m.getAgent().getIdUser(), m.getAgent());
            } else {
                userRepository.findByRole_LibelleRoleInAndIsDeletedFalse(List.of(ERole.ROLE_AGENT)).forEach(u -> destinataires.put(u.getIdUser(), u));
            }
            for (User destinataire : destinataires.values()) {
                if (notificationService.creer(destinataire.getIdUser(), TypeNotification.ECHEANCE, titre, message,
                        cible(LIEN_MANDATS, m.getIdMandat()), cle)) creees++;
            }
        }
        if (creees > 0) log.info("Alertes de fin de mandat : {} notification(s) creee(s)", creees);
    }

    // Le bailleur conteste une depense : l'agent du mandat (ou tous les agents sans agent attitre) et les admins doivent repondre
    public void depenseContestee(DepenseBien d) {
        try {
            java.util.Map<Integer, User> destinataires = new java.util.LinkedHashMap<>();
            userRepository.findByRole_LibelleRoleInAndIsDeletedFalse(List.of(ERole.ROLE_ADMIN)).forEach(u -> destinataires.put(u.getIdUser(), u));
            if (d.getMandat().getAgent() != null) {
                destinataires.put(d.getMandat().getAgent().getIdUser(), d.getMandat().getAgent());
            } else {
                userRepository.findByRole_LibelleRoleInAndIsDeletedFalse(List.of(ERole.ROLE_AGENT)).forEach(u -> destinataires.put(u.getIdUser(), u));
            }
            for (User destinataire : destinataires.values()) {
                notificationService.creer(destinataire.getIdUser(), TypeNotification.DEMANDE, "Dépense contestée par le bailleur",
                        nomComplet(d.getCour().getProprietaire()) + " conteste la dépense « " + d.getLibelle() + " » (cour "
                                + d.getCour().getReferenceCour() + ") : " + d.getMotifContestation() + ". Répondez-lui depuis le détail de la dépense.",
                        cible("/espace-agent/depenses", d.getIdDepense()), "DEPENSE_CONTESTEE:" + d.getIdDepense());
            }
        } catch (RuntimeException ex) {
            log.warn("Alerte de depense contestee non creee : {}", ex.getMessage());
        }
    }

    // Devis accepte par le bailleur mais jamais transforme en depense : rappel a l'agent (une fois par periode ecoulee)
    public boolean devisAccepteNonRealise(DevisTravaux d, long jours, long periode) {
        try {
            java.util.Set<Integer> destinataires = new java.util.LinkedHashSet<>();
            if (d.getMandat().getAgent() != null) destinataires.add(d.getMandat().getAgent().getIdUser());
            if (d.getUserCreate() != null) destinataires.add(d.getUserCreate());
            String cle = "DEVIS_ACCEPTE_RAPPEL:" + d.getIdDevis() + ":" + periode;
            boolean creee = false;
            for (Integer idUser : destinataires) {
                creee |= notificationService.creer(idUser, TypeNotification.DEMANDE, "Devis accepté à concrétiser",
                        "Le devis « " + d.getLibelle() + " » (cour " + d.getCour().getReferenceCour() + ") est accepté depuis " + jours
                                + " jours sans dépense enregistrée. Enregistrez la dépense une fois les travaux faits, ou retirez le devis.",
                        cible(LIEN_DEVIS, d.getIdDevis()), cle);
            }
            return creee;
        } catch (RuntimeException ex) {
            log.warn("Rappel d'un devis accepte non cree : {}", ex.getMessage());
            return false;
        }
    }

    // Reponse du bailleur a un devis de travaux : previent l'agent du mandat et l'auteur du devis (pas tous les agents)
    public void devisRepondu(com.immobilier.gestionImmobiliere.donnees.depenses.model.DevisTravaux d) {
        try {
            String cour = d.getCour().getReferenceCour();
            String titre;
            String message;
            switch (d.getStatut()) {
                case ACCEPTE -> {
                    titre = "Devis accepté par le bailleur";
                    message = "Le devis « " + d.getLibelle() + " » (cour " + cour + ") a été accepté : la dépense peut être enregistrée.";
                }
                case A_RENEGOCIER -> {
                    titre = "Devis à renégocier";
                    message = "Le bailleur demande de revoir le devis « " + d.getLibelle() + " » (cour " + cour + ") : " + d.getCommentaireBailleur();
                }
                case REJETE -> {
                    titre = "Devis rejeté par le bailleur";
                    message = "Le devis « " + d.getLibelle() + " » (cour " + cour + ") a été rejeté : " + d.getCommentaireBailleur();
                }
                default -> {
                    return;
                }
            }
            java.util.Set<Integer> destinataires = new java.util.LinkedHashSet<>();
            if (d.getMandat().getAgent() != null) destinataires.add(d.getMandat().getAgent().getIdUser());
            if (d.getUserCreate() != null) destinataires.add(d.getUserCreate());
            String cle = "DEVIS_REPONSE:" + d.getIdDevis() + ":" + d.getNombreSoumissions() + ":" + d.getStatut();
            for (Integer idUser : destinataires) {
                notificationService.creer(idUser, TypeNotification.DEMANDE, titre, message, cible(LIEN_DEVIS, d.getIdDevis()), cle);
            }
        } catch (RuntimeException ex) {
            log.warn("Alerte de reponse a un devis non creee : {}", ex.getMessage());
        }
    }

    // Devis reste sans reponse au-dela du delai : l'agent du mandat et l'auteur du devis peuvent le soumettre a nouveau
    public void devisExpire(com.immobilier.gestionImmobiliere.donnees.depenses.model.DevisTravaux d) {
        try {
            java.util.Set<Integer> destinataires = new java.util.LinkedHashSet<>();
            if (d.getMandat().getAgent() != null) destinataires.add(d.getMandat().getAgent().getIdUser());
            if (d.getUserCreate() != null) destinataires.add(d.getUserCreate());
            String cle = "DEVIS_EXPIRE:" + d.getIdDevis() + ":" + d.getNombreSoumissions();
            for (Integer idUser : destinataires) {
                notificationService.creer(idUser, TypeNotification.DEMANDE, "Devis expiré sans réponse",
                        "Le bailleur n'a pas répondu au devis « " + d.getLibelle() + " » (cour " + d.getCour().getReferenceCour()
                                + ") dans le délai : il a expiré. Modifiez-le pour le soumettre à nouveau, ou retirez-le.",
                        cible(LIEN_DEVIS, d.getIdDevis()), cle);
            }
        } catch (RuntimeException ex) {
            log.warn("Alerte d'expiration d'un devis non creee : {}", ex.getMessage());
        }
    }

    // Mandat resilie avec un solde de depenses : l'agent du mandat doit suivre le reglement direct du bailleur
    public void depensesDuesApresResiliation(com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat mandat, java.math.BigDecimal restant) {
        try {
            if (mandat.getAgent() == null) return;
            notificationService.creer(mandat.getAgent().getIdUser(), TypeNotification.DEMANDE, "Dépenses dues par le bailleur",
                    "Le mandat de la cour " + mandat.getCour().getReferenceCour() + " est résilié avec " + restant.stripTrailingZeros().toPlainString()
                            + " de dépenses non soldées. Ce solde sera retenu sur les derniers reversements à calculer ; "
                            + "enregistrez le règlement direct du bailleur pour le reste.",
                    "/espace-agent/depenses", "DEPENSES_DUES:" + mandat.getIdMandat());
        } catch (RuntimeException ex) {
            log.warn("Alerte de depenses dues apres resiliation non creee : {}", ex.getMessage());
        }
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
