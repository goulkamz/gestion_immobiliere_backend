package com.immobilier.gestionImmobiliere.donnees.reservations.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation_maison")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE reservation_maison SET is_deleted = true WHERE id_reservation = ?")
@Where(clause = "is_deleted = false")
public class ReservationMaison extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reservation")
    private Integer idReservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_maison", nullable = false)
    private Maison maison;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutReservation statut;
}