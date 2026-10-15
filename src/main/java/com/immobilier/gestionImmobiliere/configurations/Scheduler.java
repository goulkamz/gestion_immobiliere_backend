package com.immobilier.gestionImmobiliere.configurations;

import com.immobilier.gestionImmobiliere.modules.annonces.services.AnnonceService;
import com.immobilier.gestionImmobiliere.modules.documents.services.RapportMensuelDocumentService;
import com.immobilier.gestionImmobiliere.modules.medias.services.MediaReconciliationService;
import com.immobilier.gestionImmobiliere.modules.notifications.services.AlerteAgentService;
import com.immobilier.gestionImmobiliere.modules.notifications.services.AlerteUtilisateurService;
import com.immobilier.gestionImmobiliere.modules.notifications.services.NotificationInAppService;
import com.immobilier.gestionImmobiliere.modules.paiements.services.EcheanceGenerationService;
import com.immobilier.gestionImmobiliere.modules.paiements.services.EcheanceService;
import com.immobilier.gestionImmobiliere.modules.reservations.services.ReservationService;
import com.immobilier.gestionImmobiliere.modules.user.jwt.JwtUtils;
import com.immobilier.gestionImmobiliere.modules.user.services.PasswordResetService;
import com.immobilier.gestionImmobiliere.modules.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class Scheduler {

    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final JwtUtils jwtUtils;
    private final MediaReconciliationService mediaReconciliationService;
    private final AnnonceService annonceService;
    private final EcheanceService echeanceService;
    private final EcheanceGenerationService echeanceGenerationService;
    private final RapportMensuelDocumentService rapportMensuelDocumentService;
    private final ReservationService reservationService;
    private final NotificationInAppService notificationInAppService;
    private final AlerteAgentService alerteAgentService;
    private final AlerteUtilisateurService alerteUtilisateurService;

    // Nettoyage tous les jours a 2h du matin
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanExpiredPendingRegistrations() {
        userService.cleanExpiredPendingRegistrations();
    }

    // Nettoyage tous les jours a 2h du matin
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanExpiredPasswordResetToken() {
        passwordResetService.cleanExpiredPassordResetToken();
    }

    // Nettoyage tous les jours a 2h du matin
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanExpiredToken() {
        jwtUtils.cleanupExpiredTokens();
    }

    // dimanche 3h du matin, hors heures de charge
    @Scheduled(cron = "0 0 3 * * SUN")
    public void nettoyerFichiersOrphelins() {
        mediaReconciliationService.nettoyerFichiersOrphelins();
    }

    /**
     * Job quotidien — purge définitivement les médias soft-deleted depuis plus de
     * `retentionJours` : supprime le fichier réel sur MinIO PUIS la ligne DB (hard delete).
     * Avant ce délai, le média reste techniquement récupérable (is_deleted=true,
     * fichier toujours présent sur MinIO).
     */
    @Scheduled(cron = "0 30 2 * * *")
    public void purgerMediasSupprimes(){mediaReconciliationService.purgerMediasSupprimes();}
    // Nettoyage tous les jours a 1h du matin
    @Scheduled(cron = "0 0 1 * * *")
    public void nettoyerAnnonce(){
        annonceService.expirerAnnoncesAutomatiquement();
    }

    // Nettoyage tous les jours a 1h du matin
    @Scheduled(cron = "0 0 1 * * *")
    public void marquerChantierExpireEnRetard(){
        echeanceService.marquerEcheanceLocationEnRetard();
        echeanceService.marquerEcheanceMandatEnRetard();
    }

    /**
     * Déclenché le 31 décembre à 23h50 (avant minuit, LocalDate.now() rend
     * encore l'année en cours). Régénère les échéances de la nouvelle année
     * pour tous les contrats actifs.
     * Cron : sec min heure jour mois jourSemaine
     */
    @Scheduled(cron = "0 10 0 1 1 *")
    public void regenererEcheancesFinAnnee() {
        echeanceGenerationService.regenererEcheancesAnnuellesLocations();
    }

    /**
     * Le 1er du mois à 6h — génère le rapport définitif du mois ÉCOULÉ, quand toutes ses
     * opérations sont connues. userCreate=null indique une génération système, pas une
     * action manuelle d'un admin.
     */
    @Scheduled(cron = "0 0 6 1 * *")
    public void genererRapportDuMois() {
        LocalDate moisEcoule = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        rapportMensuelDocumentService.genererOuRecuperer(moisEcoule, null);
    }

    /**
     * Toutes les heures — annule les réservations EN_ATTENTE dont le délai
     * d'expiration (dateDebut + DELAI_EXPIRATION_RESERVATION_HEURES) est dépassé,
     * et repasse la maison en DISPONIBLE. Fréquence horaire car le délai par
     * défaut (6h) rendrait un contrôle quotidien trop grossier.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void expirerReservationsEnAttente() {
        reservationService.expirerReservationsEnAttente();
    }

    // Tous les jours a 7h - alerte les agents des locations de biens/services qui se terminent bientot ou sont depassees
    @Scheduled(cron = "${app.notifications.cron-agents:0 0 7 * * *}")
    public void alerterLocationsAEcheance() {
        alerteAgentService.alerterLocationsAEcheance();
    }

    // Tous les jours a 7h15 - alerte les clients : loyers proches ou en retard, fin de location de bien/service
    @Scheduled(cron = "${app.notifications.cron-clients:0 15 7 * * *}")
    public void alerterClients() {
        alerteUtilisateurService.alerterClients();
    }

    // Tous les jours a 3h30 - purge les notifications lues depuis plus de 30 jours (les non lues sont conservees)
    @Scheduled(cron = "0 30 3 * * *")
    public void purgerNotificationsLues() {
        notificationInAppService.purgerNotificationsLues();
    }
}