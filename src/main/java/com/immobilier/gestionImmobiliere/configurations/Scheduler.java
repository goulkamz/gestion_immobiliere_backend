package com.immobilier.gestionImmobiliere.configurations;

import com.immobilier.gestionImmobiliere.modules.annonces.services.AnnonceService;
import com.immobilier.gestionImmobiliere.modules.documents.services.RapportMensuelDocumentService;
import com.immobilier.gestionImmobiliere.modules.medias.services.MediaReconciliationService;
import com.immobilier.gestionImmobiliere.modules.paiements.services.EcheanceGenerationService;
import com.immobilier.gestionImmobiliere.modules.paiements.services.EcheanceService;
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
     * Le 15 du mois à 6h — on suppose que tous les paiements/virements du
     * mois sont déjà effectués à cette heure. userCreate=null indique une
     * génération système, pas une action manuelle d'un admin.
     */
    @Scheduled(cron = "0 0 6 15 * *")
    public void genererRapportDuMois() {
        LocalDate moisEnCours = LocalDate.now().withDayOfMonth(1);
        rapportMensuelDocumentService.genererOuRecuperer(moisEnCours, null);
    }
}