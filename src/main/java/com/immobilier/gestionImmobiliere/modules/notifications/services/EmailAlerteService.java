package com.immobilier.gestionImmobiliere.modules.notifications.services;

import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.modules.user.services.NotificationService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Envoi des e-mails d'alerte (tranche 4). Hors du thread de la requete : un SMTP lent ou en panne ne bloque
// ni n'invalide jamais l'action metier. Si une adresse de redirection est configuree (environnement de test,
// ou comptes de demonstration aux adresses factices), tous les mails lui sont envoyes a la place du destinataire.
@Service
public class EmailAlerteService {

    private static final Logger log = LoggerFactory.getLogger(EmailAlerteService.class);

    private final NotificationService notificationService;
    private final boolean actif;
    private final String redirection;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public EmailAlerteService(NotificationService notificationService,
                              @Value("${app.notifications.email-actif:true}") boolean actif,
                              @Value("${app.notifications.email-redirection:}") String redirection) {
        this.notificationService = notificationService;
        this.actif = actif;
        this.redirection = redirection == null ? "" : redirection.trim();
    }

    public void envoyer(User destinataire, String titre, String message) {
        if (!actif || destinataire == null || destinataire.getEmail() == null) return;
        String adresse = redirection.isEmpty() ? destinataire.getEmail() : redirection;
        String nom = ((destinataire.getPrenom() == null ? "" : destinataire.getPrenom()) + " "
                + (destinataire.getNom() == null ? "" : destinataire.getNom())).trim();
        executor.submit(() -> {
            try {
                notificationService.envoyerAlerte(adresse, nom, titre, message);
            } catch (RuntimeException e) {
                log.warn("E-mail d'alerte non envoye a {} : {}", adresse, e.getMessage());
            }
        });
    }

    @PreDestroy
    void arreter() {
        executor.shutdown();
    }
}
