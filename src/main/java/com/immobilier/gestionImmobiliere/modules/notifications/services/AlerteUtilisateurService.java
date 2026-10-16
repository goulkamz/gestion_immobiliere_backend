package com.immobilier.gestionImmobiliere.modules.notifications.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.Avenant;
import com.immobilier.gestionImmobiliere.donnees.biens.model.LocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.LocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.notifications.model.TypeNotification;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.reservations.model.ReservationMaison;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Alertes in-app destinees aux clients et aux bailleurs (tranche 3). Le destinataire est toujours deduit des donnees
// metier (locataire, client, proprietaire de la cour), jamais d'un identifiant venu de la requete.
@Service
public class AlerteUtilisateurService {

    private static final Logger log = LoggerFactory.getLogger(AlerteUtilisateurService.class);
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int JOURS_PREAVIS_ECHEANCE_LOYER = 3;
    private static final int JOURS_PREAVIS_LOCATION = 2;
    private static final int JOURS_PREAVIS_MANDAT = 30;

    private static final String LIEN_RESERVATIONS = "/espace-client/reservations";
    private static final String LIEN_LOCATIONS = "/espace-client/locations";
    private static final String LIEN_ECHEANCES = "/espace-client/echeances";
    private static final String LIEN_REMBOURSEMENTS = "/espace-client/remboursements";
    private static final String LIEN_BAILLEUR_MANDATS = "/espace-bailleur/mandats";
    private static final String LIEN_CONTRATS = "/espace-client/contrats";
    private static final String LIEN_BAILLEUR_CONTRATS = "/espace-bailleur/contrats";
    private static final String LIEN_BAILLEUR_ECHEANCES = "/espace-bailleur/echeances";

    private final NotificationInAppService notificationService;
    private final EcheanceLoyerRepository echeanceRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final ContratMandatRepository contratMandatRepository;
    private final LocationBienServiceRepository locationRepository;
    private final EmailAlerteService emailService;

    public AlerteUtilisateurService(NotificationInAppService notificationService, EcheanceLoyerRepository echeanceRepository,
                                    ContratLocationRepository contratLocationRepository, ContratMandatRepository contratMandatRepository,
                                    LocationBienServiceRepository locationRepository, EmailAlerteService emailService) {
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.echeanceRepository = echeanceRepository;
        this.contratLocationRepository = contratLocationRepository;
        this.contratMandatRepository = contratMandatRepository;
        this.locationRepository = locationRepository;
    }

    // ---- Evenements declenches par une action (une alerte ne doit jamais faire echouer l'action metier) ----

    public void reservationConfirmee(ReservationMaison r) {
        reservation(r, "RESERVATION_CONFIRMEE", "Réservation confirmée",
                "Votre réservation de " + r.getMaison().getNomCommunMaison() + " est confirmée.");
    }

    public void reservationAnnulee(ReservationMaison r, boolean expiree) {
        reservation(r, "RESERVATION_ANNULEE", expiree ? "Réservation expirée" : "Réservation annulée",
                "Votre réservation de " + r.getMaison().getNomCommunMaison()
                        + (expiree ? " a expiré faute de confirmation." : " a été annulée."));
    }

    private void reservation(ReservationMaison r, String prefixeCle, String titre, String message) {
        envoyer(r.getUser(), false, TypeNotification.DEMANDE, titre, message, cible(LIEN_RESERVATIONS, r.getIdReservation()),
                prefixeCle + ":" + r.getIdReservation());
    }

    public void locationConfirmee(LocationBienService l) {
        location(l, "LOCATION_CONFIRMEE", "Location confirmée", "Votre location de " + l.getBienService().getLibelle()
                + " est confirmée du " + l.getDateDebut().format(FORMAT_DATE) + " au " + l.getDateFin().format(FORMAT_DATE) + ".");
    }

    public void locationAnnulee(LocationBienService l) {
        location(l, "LOCATION_ANNULEE", "Demande de location annulée",
                "Votre demande de location de " + l.getBienService().getLibelle() + " a été annulée.");
    }

    public void locationTerminee(LocationBienService l) {
        location(l, "LOCATION_TERMINEE", "Location terminée",
                "Votre location de " + l.getBienService().getLibelle() + " est terminée.");
    }

    private void location(LocationBienService l, String prefixeCle, String titre, String message) {
        envoyer(l.getClient(), false, TypeNotification.DEMANDE, titre, message, cible(LIEN_LOCATIONS, l.getIdLocationBienService()),
                prefixeCle + ":" + l.getIdLocationBienService());
    }

    public void remboursementEffectue(LocationBienService l, Integer idRemboursement, BigDecimal montant) {
        envoyer(l.getClient(), true, TypeNotification.INFO, "Remboursement effectué",
                "Un remboursement de " + montant.stripTrailingZeros().toPlainString() + " vous a été versé pour la location de "
                        + l.getBienService().getLibelle() + ".", LIEN_REMBOURSEMENTS, "REMBOURSEMENT:" + idRemboursement);
    }

    // Avenant ajoute a un contrat actif : le locataire et le bailleur concernes sont informes (document a consulter)
    public void avenantCree(Avenant a) {
        String cle = "AVENANT:" + a.getIdAvenant();
        String effet = a.getDateEffet().format(FORMAT_DATE);
        if (a.getContratLocation() != null) {
            ContratLocation c = a.getContratLocation();
            String message = "Un avenant n°" + a.getNumeroOrdre() + " a été ajouté au contrat de location de "
                    + c.getMaison().getNomCommunMaison() + ", à compter du " + effet + ".";
            envoyer(c.getLocataire(), false, TypeNotification.INFO, "Avenant à votre contrat", message, cible(LIEN_CONTRATS, c.getIdContratLocation()), cle);
            User bailleur = c.getMaison().getCour().getProprietaire();
            if (bailleur != null) {
                envoyer(bailleur, false, TypeNotification.INFO, "Avenant à un contrat de location", message, cible(LIEN_BAILLEUR_CONTRATS, c.getIdContratLocation()), cle);
            }
        } else if (a.getMandat() != null) {
            ContratMandat m = a.getMandat();
            User bailleur = m.getCour().getProprietaire();
            if (bailleur != null) {
                envoyer(bailleur, false, TypeNotification.INFO, "Avenant à votre mandat",
                        "Un avenant n°" + a.getNumeroOrdre() + " a été ajouté à votre mandat sur la cour "
                                + m.getCour().getReferenceCour() + ", à compter du " + effet + ".", cible(LIEN_BAILLEUR_MANDATS, m.getIdMandat()), cle);
            }
        }
    }

    // Virement du loyer reverse au bailleur (confirme par l'agent)
    public void virementMandatConfirme(EcheanceLoyer echeance) {
        try {
            ContratMandat mandat = contratMandatRepository.findById(echeance.getEntiteEcheanceId()).orElse(null);
            if (mandat == null || mandat.getCour().getProprietaire() == null) return;
            String message = "Un virement de " + echeance.getMontantDu().stripTrailingZeros().toPlainString()
                    + " vous a été effectué (échéance du " + echeance.getDateEcheance().format(FORMAT_DATE) + ").";
            notificationService.creer(mandat.getCour().getProprietaire().getIdUser(), TypeNotification.INFO, "Virement effectué",
                    message, LIEN_BAILLEUR_ECHEANCES, "VIREMENT_MANDAT:" + echeance.getIdEcheance());
        } catch (RuntimeException e) {
            log.warn("Alerte de virement bailleur non creee : {}", e.getMessage());
        }
    }

    // Montant a reverser calcule par l'agent : le bailleur sait qu'un virement est a venir
    public void reversementCalcule(ContratMandat mandat, BigDecimal montantNet, String periode, String cle) {
        User bailleur = mandat.getCour().getProprietaire();
        if (bailleur == null) return;
        envoyer(bailleur, false, TypeNotification.INFO, "Montant à reverser calculé",
                "Le montant à vous reverser pour " + periode + " est de " + montantNet.stripTrailingZeros().toPlainString()
                        + " (cour " + mandat.getCour().getReferenceCour() + ").", LIEN_BAILLEUR_ECHEANCES, cle);
    }

    // ---- Job quotidien ----

    /**
     * Echeances de loyer proches ou en retard (client locataire) et locations de biens/services qui se
     * terminent bientot (client). Cles anti-doublons : une alerte par echeance et par etat, une par date de fin.
     */
    @Transactional
    public void alerterClients() {
        LocalDate aujourdhui = LocalDate.now();
        int creees = 0;

        // Echeances a venir (ou depassees mais encore dans la tolerance) : EN_ATTENTE
        List<EcheanceLoyer> proches = echeanceRepository.findByEntiteEcheanceTypeAndStatutAndDateEcheanceBefore(
                TypeEcheance.LOCATION, StatutEcheance.EN_ATTENTE, aujourdhui.plusDays(JOURS_PREAVIS_ECHEANCE_LOYER + 1L));
        for (EcheanceLoyer e : proches) {
            ContratLocation contrat = contratLocationRepository.findById(e.getEntiteEcheanceId()).orElse(null);
            if (contrat == null) continue;
            String quand = e.getDateEcheance().isBefore(aujourdhui) ? "était attendu le " : "est attendu le ";
            if (envoyer(contrat.getLocataire(), true, TypeNotification.ECHEANCE, "Loyer à payer",
                    "Votre loyer de " + contrat.getMaison().getNomCommunMaison() + " " + quand + e.getDateEcheance().format(FORMAT_DATE) + ".",
                    LIEN_ECHEANCES, "ECHEANCE_PROCHE:" + e.getIdEcheance())) creees++;
        }

        // Echeances en retard
        List<EcheanceLoyer> enRetard = echeanceRepository.findByEntiteEcheanceTypeAndStatutAndDateEcheanceBefore(
                TypeEcheance.LOCATION, StatutEcheance.EN_RETARD, aujourdhui);
        for (EcheanceLoyer e : enRetard) {
            ContratLocation contrat = contratLocationRepository.findById(e.getEntiteEcheanceId()).orElse(null);
            if (contrat == null) continue;
            if (envoyer(contrat.getLocataire(), true, TypeNotification.ECHEANCE, "Loyer en retard",
                    "Votre loyer de " + contrat.getMaison().getNomCommunMaison() + " du " + e.getDateEcheance().format(FORMAT_DATE)
                            + " est en retard.", LIEN_ECHEANCES, "ECHEANCE_RETARD:" + e.getIdEcheance())) creees++;
        }

        // Fin de location de bien/service
        List<LocationBienService> locations = locationRepository.findByStatutAndDateFinBefore(
                StatutLocationBienService.ACTIF, aujourdhui.plusDays(JOURS_PREAVIS_LOCATION + 1L).atStartOfDay());
        for (LocationBienService l : locations) {
            LocalDate fin = l.getDateFin().toLocalDate();
            boolean depassee = fin.isBefore(aujourdhui);
            String message = "Votre location de " + l.getBienService().getLibelle() + (depassee ? " est terminée depuis le " : " se termine le ")
                    + fin.format(FORMAT_DATE) + ".";
            if (envoyer(l.getClient(), true, TypeNotification.ECHEANCE, depassee ? "Location terminée" : "Fin de location proche",
                    message, cible(LIEN_LOCATIONS, l.getIdLocationBienService()), "CLIENT_LOCATION_FIN:" + l.getIdLocationBienService() + ":" + fin)) creees++;
        }
        creees += alerterMandatsBailleurs(aujourdhui);
        if (creees > 0) log.info("Alertes client : {} notification(s) creee(s)", creees);
    }

    // Mandat ACTIF qui se termine dans les JOURS_PREAVIS_MANDAT prochains jours : alerte le bailleur concerne
    private int alerterMandatsBailleurs(LocalDate aujourdhui) {
        int creees = 0;
        for (ContratMandat m : contratMandatRepository.findByStatut(StatutMandat.ACTIF)) {
            if (m.getDateFin() == null || m.getCour().getProprietaire() == null) continue;
            LocalDate fin = m.getDateFin().toLocalDate();
            if (fin.isAfter(aujourdhui.plusDays(JOURS_PREAVIS_MANDAT))) continue;
            boolean depasse = fin.isBefore(aujourdhui);
            if (envoyer(m.getCour().getProprietaire(), false, TypeNotification.ECHEANCE,
                    depasse ? "Mandat arrivé à terme" : "Mandat bientôt terminé",
                    "Votre mandat sur la cour " + m.getCour().getReferenceCour() + (depasse ? " est arrivé à terme le " : " se termine le ")
                            + fin.format(FORMAT_DATE) + ".", cible(LIEN_BAILLEUR_MANDATS, m.getIdMandat()), "MANDAT_FIN:" + m.getIdMandat() + ":" + fin)) creees++;
        }
        return creees;
    }

    // Lien vers la page, avec l'identifiant de la ligne concernee (la page la repere et la met en evidence)
    private static String cible(String lien, Integer id) {
        return id == null ? lien : lien + "?cible=" + id;
    }

    // Mail envoye uniquement si la notification vient d'etre creee (pas de renvoi sur une cle deja connue)
    private boolean envoyer(User u, boolean email, TypeNotification type, String titre, String message, String lien, String cle) {
        try {
            boolean creee = notificationService.creer(u.getIdUser(), type, titre, message, lien, cle);
            if (creee && email) emailService.envoyer(u, titre, message);
            return creee;
        } catch (RuntimeException e) {
            log.warn("Alerte {} non creee : {}", cle, e.getMessage());
            return false;
        }
    }
}
