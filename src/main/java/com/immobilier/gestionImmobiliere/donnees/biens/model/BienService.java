package com.immobilier.gestionImmobiliere.donnees.biens.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import com.immobilier.gestionImmobiliere.donnees.localisation.model.Secteur;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.List;

@Entity
@Table(name = "bien_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE bien_service SET is_deleted = true WHERE id_bien_service = ?")
@Where(clause = "is_deleted = false")
public class BienService extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_bien_service")
    @SequenceGenerator(name = "seq_bien_service", sequenceName = "seq_bien_service", allocationSize = 1)
    @Column(name = "id_bien_service")
    private Integer idBienService;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_secteur", nullable = false)
    private Secteur secteur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categorie", nullable = false)
    private CategorieBienService categorie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User gestionnaire;

    @Column(name = "libelle")
    private String libelle;

    @Column(name = "description")
    private String description;

    @Column(name = "prix_journalier")
    private Double prixJournalier;

    @Column(name = "prix_mensuel")
    private Double prixMensuel;

    @Enumerated(EnumType.STRING)
    @Column(name = "disponibilite", length = 254)
    private StatutBienService disponibilite;

    @Column(name = "user_create")
    private Integer userCreate;

    @Column(name = "user_update")
    private Integer userUpdate;

    @OneToMany(mappedBy = "bienService", fetch = FetchType.LAZY)
    private List<LocationBienService> locations;


}
