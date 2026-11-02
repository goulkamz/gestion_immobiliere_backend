package com.immobilier.gestionImmobiliere.modules.notifications.services;

import com.immobilier.gestionImmobiliere.donnees.notifications.model.TypeNotification;
import com.immobilier.gestionImmobiliere.donnees.user.model.ERole;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

// Supervision des sauvegardes : le service de sauvegarde ecrit son resultat dans <dossier>/dernier-statut.txt
// (« OK|ECHEC <date iso> <fichier> »). Si la derniere sauvegarde a echoue, ou si rien n'a tourne depuis plus de
// delaiHeures, les administrateurs recoivent une notification et un e-mail (une fois par jour et par probleme).
// Sans dossier configure (developpement), la surveillance est inactive.
@Service
public class AlerteSauvegardeService {

    private static final Logger log = LoggerFactory.getLogger(AlerteSauvegardeService.class);
    static final String FICHIER_STATUT = "dernier-statut.txt";
    private static final String LIEN_ADMIN = "/espace-admin";

    /** Probleme detecte : code (cle anti-doublon) et message lisible. */
    public record Probleme(String code, String titre, String message) {}

    private final NotificationInAppService notificationService;
    private final EmailAlerteService emailService;
    private final UserRepository userRepository;
    private final String dossier;
    private final long delaiHeures;

    public AlerteSauvegardeService(NotificationInAppService notificationService, EmailAlerteService emailService,
                                   UserRepository userRepository,
                                   @Value("${app.sauvegardes.dossier:}") String dossier,
                                   @Value("${app.sauvegardes.delai-heures:26}") long delaiHeures) {
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.dossier = dossier == null ? "" : dossier.trim();
        this.delaiHeures = delaiHeures;
    }

    /** Verifie l'etat des sauvegardes et previent les administrateurs ; retourne le nombre de notifications creees. */
    public int verifier() {
        if (dossier.isEmpty()) return 0;
        Optional<Probleme> probleme = evaluer(Path.of(dossier, FICHIER_STATUT), Instant.now(), delaiHeures);
        if (probleme.isEmpty()) return 0;

        Probleme p = probleme.get();
        String cle = "SAUVEGARDE:" + p.code() + ":" + LocalDate.now();
        int creees = 0;
        List<User> admins = userRepository.findByRole_LibelleRoleInAndIsDeletedFalse(List.of(ERole.ROLE_ADMIN));
        for (User admin : admins) {
            try {
                boolean creee = notificationService.creer(admin.getIdUser(), TypeNotification.INFO, p.titre(), p.message(), LIEN_ADMIN, cle);
                if (creee) {
                    emailService.envoyer(admin, p.titre(), p.message());
                    creees++;
                }
            } catch (RuntimeException e) {
                log.warn("Alerte de sauvegarde non creee pour {} : {}", admin.getEmail(), e.getMessage());
            }
        }
        log.error("Sauvegarde en defaut ({}) : {} administrateur(s) prevenu(s)", p.code(), creees);
        return creees;
    }

    /** Decision pure, sans base ni horloge cachee : testable avec un fichier temporaire. */
    static Optional<Probleme> evaluer(Path fichierStatut, Instant maintenant, long delaiHeures) {
        String contenu;
        try {
            contenu = Files.readString(fichierStatut).trim();
        } catch (IOException e) {
            return Optional.of(new Probleme("ABSENT", "Sauvegarde introuvable",
                    "Aucun résultat de sauvegarde n'a été trouvé. Le service de sauvegarde est peut-être arrêté ou n'a jamais tourné."));
        }

        String[] morceaux = contenu.split("\\s+", 3);
        String statut = morceaux.length > 0 ? morceaux[0] : "";
        Instant date;
        try {
            date = OffsetDateTime.parse(morceaux[1]).toInstant();
        } catch (DateTimeParseException | ArrayIndexOutOfBoundsException e) {
            return Optional.of(new Probleme("ILLISIBLE", "Sauvegarde : état illisible",
                    "Le fichier d'état de la sauvegarde est illisible : « " + contenu + " »."));
        }

        if (!"OK".equals(statut)) {
            return Optional.of(new Probleme("ECHEC", "La dernière sauvegarde a échoué",
                    "La sauvegarde du " + date + " a échoué. Consulter les journaux du service de sauvegarde (docker logs gestion-immo-sauvegarde)."));
        }

        long heures = Duration.between(date, maintenant).toHours();
        if (heures > delaiHeures) {
            return Optional.of(new Probleme("RETARD", "Sauvegarde en retard",
                    "La dernière sauvegarde réussie date de " + heures + " heures (limite : " + delaiHeures
                            + " heures). Vérifier que le service de sauvegarde tourne."));
        }
        return Optional.empty();
    }
}
