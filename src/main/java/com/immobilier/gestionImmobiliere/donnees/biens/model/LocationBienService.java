package com.immobilier.gestionImmobiliere.donnees.biens.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.PaiementLocationBienService;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "location_bien_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE location_bien_service SET is_deleted = true WHERE id_location_bien_service = ?")
@Where(clause = "is_deleted = false")
public class LocationBienService extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_location_service")
    @SequenceGenerator(name = "seq_location_service", sequenceName = "seq_location_service", allocationSize = 1)
    @Column(name = "id_location_bien_service")
    private Integer idLocationBienService;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User client;

    @OneToMany(mappedBy = "location", fetch = FetchType.LAZY)
    private List<PaiementLocationBienService> paiements;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_bien_service", nullable = false)
    private BienService bienService;

    @Column(name = "destination")
    private String destination;

    @Column(name = "date_debut")
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(name = "duree")
    private Integer duree;

    @Column(name = "montant_total")
    private Double montantTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 254)
    private StatutLocationBienService statut;

    @Column(name = "user_create")
    private Integer userCreate;

    @Column(name = "user_update")
    private Integer userUpdate;

}
