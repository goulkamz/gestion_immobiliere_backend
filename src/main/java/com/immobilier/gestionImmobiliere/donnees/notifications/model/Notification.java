package com.immobilier.gestionImmobiliere.donnees.notifications.model;

import com.immobilier.gestionImmobiliere.donnees.Model_1;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data @SuperBuilder
@NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE notification SET is_deleted = true WHERE id_notification = ?")
@SQLRestriction("is_deleted = false")
public class Notification extends Model_1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notification")
    private Integer idNotification;

    // Destinataire unique : une alerte pour plusieurs utilisateurs = une ligne par utilisateur
    @Column(name = "id_user", nullable = false)
    private Integer idUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TypeNotification type;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "message")
    private String message;

    // Route frontend vers laquelle la notification renvoie (ex. /espace-agent/locations)
    @Column(name = "lien")
    private String lien;

    // Anti-doublons des alertes automatiques (ex. LOCATION_ECHEANCE:11:J-2), unique par utilisateur
    @Column(name = "cle_alerte")
    private String cleAlerte;

    @Column(name = "lue", nullable = false)
    @Builder.Default
    private Boolean lue = false;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;
}
