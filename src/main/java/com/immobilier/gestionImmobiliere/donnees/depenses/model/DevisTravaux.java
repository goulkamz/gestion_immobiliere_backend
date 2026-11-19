package com.immobilier.gestionImmobiliere.donnees.depenses.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import com.immobilier.gestionImmobiliere.donnees.biens.model.Cour;
import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Devis de travaux à la charge du bailleur, proposé par l'agence sur une cour (ou une de ses maisons)
 * sous mandat de gestion. Le bailleur l'accepte, demande une renégociation ou le rejette ; accepté,
 * il donne lieu à une dépense retenue sur les reversements.
 */
@Entity
@Table(name = "devis_travaux")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@SQLDelete(sql = "UPDATE devis_travaux SET is_deleted = true WHERE id_devis = ?")
@SQLRestriction("is_deleted = false")
public class DevisTravaux extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_devis")
    private Integer idDevis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_mandat", nullable = false)
    private ContratMandat mandat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cour", nullable = false)
    private Cour cour;

    /** Renseignée si les travaux ne concernent qu'une maison ; null = la cour entière (parties communes). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_maison")
    private Maison maison;

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false)
    private CategorieDepense categorie;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "prestataire")
    private String prestataire;

    @Column(name = "montant", nullable = false)
    private BigDecimal montant;

    /** Étalement proposé : nombre de reversements sur lesquels la dépense serait retenue (1 = d'un coup). */
    @Column(name = "nombre_mois", nullable = false)
    @Builder.Default
    private Integer nombreMois = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutDevis statut = StatutDevis.EN_ATTENTE;

    /** Dernière réponse du bailleur : ce qu'il demande de revoir, ou pourquoi il rejette. */
    @Column(name = "commentaire_bailleur", length = 500)
    private String commentaireBailleur;

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    /** 1 à la création, +1 à chaque nouvelle soumission après une demande de renégociation. */
    @Column(name = "nombre_soumissions", nullable = false)
    @Builder.Default
    private Integer nombreSoumissions = 1;

    /** Soumission en cours (création, puis chaque nouvelle soumission) : point de départ du délai de réponse. */
    @Column(name = "date_soumission", nullable = false)
    @Builder.Default
    private LocalDateTime dateSoumission = LocalDateTime.now();

    /**
     * Accord donné par le bailleur en dehors de la plateforme (téléphone, en personne...), enregistré par l'agent
     * avec le détail dans {@code accordMotif}. Le bailleur en est prévenu et peut le contester tant que la dépense
     * n'est pas enregistrée.
     */
    @Column(name = "accord_hors_plateforme", nullable = false)
    @Builder.Default
    private Boolean accordHorsPlateforme = false;

    @Column(name = "accord_motif")
    private String accordMotif;

    @Column(name = "accord_date")
    private java.time.LocalDate accordDate;

    @Column(name = "accord_user")
    private Integer accordUser;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @Column(name = "user_create")
    private Integer userCreate;

    @Column(name = "user_update")
    private Integer userUpdate;
}
