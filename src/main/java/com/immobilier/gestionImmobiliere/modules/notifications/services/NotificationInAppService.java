package com.immobilier.gestionImmobiliere.modules.notifications.services;

import com.immobilier.gestionImmobiliere.donnees.notifications.model.Notification;
import com.immobilier.gestionImmobiliere.donnees.notifications.model.TypeNotification;
import com.immobilier.gestionImmobiliere.donnees.notifications.repository.NotificationRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.notifications.dto.responses.NotificationResponseDTO;
import com.immobilier.gestionImmobiliere.utils.CustomDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

// Notifications in-app (cloche). Distinct de NotificationService, qui n'envoie que des e-mails de comptes.
@Service
public class NotificationInAppService {

    private static final Logger log = LoggerFactory.getLogger(NotificationInAppService.class);

    private final NotificationRepository notificationRepository;
    private final int retentionLuesJours;

    public NotificationInAppService(NotificationRepository notificationRepository,
                                    @Value("${app.notifications.retention-lues-jours:30}") int retentionLuesJours) {
        this.notificationRepository = notificationRepository;
        this.retentionLuesJours = retentionLuesJours;
    }

    /**
     * Cree une notification pour un destinataire. Si cleAlerte est renseignee et deja connue pour ce
     * destinataire, rien n'est cree (anti-doublons des alertes automatiques). Point d'entree pour les
     * tranches suivantes (alertes agent/client/bailleur) ; un envoi a plusieurs = un appel par destinataire.
     * @return true si une notification a ete creee
     */
    @Transactional
    public boolean creer(Integer idUser, TypeNotification type, String titre, String message, String lien, String cleAlerte) {
        if (cleAlerte != null && notificationRepository.existsByIdUserAndCleAlerte(idUser, cleAlerte)) {
            return false;
        }
        notificationRepository.save(Notification.builder()
                .idUser(idUser).type(type).titre(titre).message(message).lien(lien).cleAlerte(cleAlerte)
                .build());
        return true;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getMesNotifications(Integer idUser, boolean nonLuesSeulement, Pageable pageable) {
        Page<Notification> page = nonLuesSeulement
                ? notificationRepository.findByIdUserAndLue(idUser, false, pageable)
                : notificationRepository.findByIdUser(idUser, pageable);
        return buildSuccessResponse(HttpStatus.OK, "Liste des notifications", "NOTIFICATION_LIST", page.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> countNonLues(Integer idUser) {
        return buildSuccessResponse(HttpStatus.OK, "Nombre de notifications non lues", "NOTIFICATION_UNREAD_COUNT",
                Map.of("count", notificationRepository.countByIdUserAndLueFalse(idUser)));
    }

    // 404 (et non 403) si la notification appartient a un autre utilisateur : on ne revele pas son existence
    @Transactional
    public ResponseEntity<?> marquerLue(Integer id, Integer idUser) {
        Notification notification = notificationRepository.findByIdNotificationAndIdUser(id, idUser)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (!Boolean.TRUE.equals(notification.getLue())) {
            notification.setLue(true);
            notification.setDateLecture(CustomDate.now());
            notificationRepository.save(notification);
        }
        return buildSuccessResponse(HttpStatus.OK, "Notification marquée comme lue", "NOTIFICATION_READ", toDto(notification));
    }

    @Transactional
    public ResponseEntity<?> marquerToutesLues(Integer idUser) {
        int nb = notificationRepository.marquerToutesLues(idUser, CustomDate.now());
        return buildSuccessResponse(HttpStatus.OK, "Notifications marquées comme lues", "NOTIFICATION_ALL_READ", Map.of("count", nb));
    }

    // Job quotidien : supprime les notifications LUES depuis plus de retentionLuesJours ; les non lues restent
    @Transactional
    public void purgerNotificationsLues() {
        LocalDateTime limite = CustomDate.now().minusDays(retentionLuesJours);
        int nb = notificationRepository.purgerLuesAvant(limite);
        if (nb > 0) log.info("Purge des notifications lues : {} supprimee(s)", nb);
    }

    private NotificationResponseDTO toDto(Notification n) {
        return NotificationResponseDTO.builder()
                .idNotification(n.getIdNotification()).type(n.getType()).titre(n.getTitre())
                .message(n.getMessage()).lien(n.getLien()).lue(n.getLue()).dateCreation(n.getCreatedAt())
                .build();
    }
}
