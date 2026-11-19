package com.immobilier.gestionImmobiliere.donnees.depenses.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Règlement fait directement par le bailleur à l'agence pour une dépense qui ne peut plus être retenue
 * sur des reversements (mandat résilié). Enregistré par l'agent ; plusieurs règlements partiels possibles.
 */
@Entity
@Table(name = "depense_reglement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@SQLDelete(sql = "UPDATE depense_reglement SET is_deleted = true WHERE id_reglement = ?")
@SQLRestriction("is_deleted = false")
public class DepenseReglement extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reglement")
    private Integer idReglement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_depense", nullable = false)
    private DepenseBien depense;

    @Column(name = "montant", nullable = false)
    private BigDecimal montant;

    @Column(name = "date_reglement", nullable = false)
    private LocalDate dateReglement;

    /** Espèces, virement, mobile money... (texte libre). */
    @Column(name = "mode_reglement", length = 50)
    private String modeReglement;

    @Column(name = "reference")
    private String reference;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @Column(name = "user_create")
    private Integer userCreate;

    /** Règlement saisi par erreur : il reste en base (preuve), mais ne compte plus dans ce que le bailleur a payé. */
    @Column(name = "annule", nullable = false)
    @Builder.Default
    private Boolean annule = false;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @Column(name = "date_annulation")
    private java.time.LocalDateTime dateAnnulation;

    @Column(name = "user_annulation")
    private Integer userAnnulation;
}
