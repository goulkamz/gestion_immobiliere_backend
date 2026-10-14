package com.immobilier.gestionImmobiliere.modules.notifications.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.notifications.model.TypeNotification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class NotificationResponseDTO {
    private Integer idNotification;
    private TypeNotification type;
    private String titre;
    private String message;
    private String lien;
    private Boolean lue;
    private LocalDateTime dateCreation;
}
