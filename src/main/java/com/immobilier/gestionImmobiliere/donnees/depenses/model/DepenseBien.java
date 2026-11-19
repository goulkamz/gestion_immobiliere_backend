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
import java.time.LocalDate;

/**
 * Dépense d'une cour (ou d'une de ses maisons) à la charge du bailleur, avancée par l'agence
 * et retenue sur les reversements du mandat de gestion, d'un coup ou étalée sur plusieurs mois.
 */
@Entity
@Table(name = "depense_bien")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@SQLDelete(sql = "UPDATE depense_bien SET is_deleted = true WHERE id_depense = ?")
@SQLRestriction("is_deleted = false")
public class DepenseBien extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_depense")
    private Integer idDepense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_mandat", nullable = false)
    private ContratMandat mandat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cour", nullable = false)
    private Cour cour;

    /** Renseignée si la dépense ne concerne qu'une maison ; null = la cour entière (parties communes). */
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

    @Column(name = "date_depense", nullable = false)
    private LocalDate dateDepense;

    @Column(name = "montant", nullable = false)
    private BigDecimal montant;

    /** Nombre de reversements sur lesquels la dépense est retenue (1 = d'un coup). */
    @Column(name = "nombre_mois", nullable = false)
    @Builder.Default
    private Integer nombreMois = 1;

    /** Nombre de reversements déjà passés sur cette dépense. */
    @Column(name = "mois_ecoules", nullable = false)
    @Builder.Default
    private Integer moisEcoules = 0;

    @Column(name = "montant_impute", nullable = false)
    @Builder.Default
    private BigDecimal montantImpute = BigDecimal.ZERO;

    /** Total réglé directement par le bailleur, hors reversement (mandat résilié). */
    @Column(name = "montant_regle", nullable = false)
    @Builder.Default
    private BigDecimal montantRegle = BigDecimal.ZERO;

    /**
     * Rééchelonnement : à partir du reversement de rang {@code baseMois}, le solde (montant - baseMontant)
     * est retenu sur (nombreMois - baseMois) reversements. Sans rééchelonnement, les deux valent 0.
     */
    @Column(name = "base_montant", nullable = false)
    @Builder.Default
    private BigDecimal baseMontant = BigDecimal.ZERO;

    @Column(name = "base_mois", nullable = false)
    @Builder.Default
    private Integer baseMois = 0;

    /** Étalement d'origine, renseigné au premier rééchelonnement (null = jamais rééchelonnée). */
    @Column(name = "nombre_mois_initial")
    private Integer nombreMoisInitial;

    @Column(name = "motif_reechelonnement")
    private String motifReechelonnement;

    @Column(name = "date_reechelonnement")
    private java.time.LocalDateTime dateReechelonnement;

    /** Reste dû que l'agence renonce à réclamer (abandon de créance, décidé par l'admin) : la dépense est alors soldée. */
    @Column(name = "montant_abandonne", nullable = false)
    @Builder.Default
    private BigDecimal montantAbandonne = BigDecimal.ZERO;

    @Column(name = "motif_abandon")
    private String motifAbandon;

    @Column(name = "date_abandon")
    private java.time.LocalDateTime dateAbandon;

    @Column(name = "user_abandon")
    private Integer userAbandon;

    /** Rétablissement d'une créance abandonnée par erreur (admin) : trace conservée, le montant abandonné repasse à 0. */
    @Column(name = "motif_retablissement")
    private String motifRetablissement;

    @Column(name = "date_retablissement")
    private java.time.LocalDateTime dateRetablissement;

    @Column(name = "user_retablissement")
    private Integer userRetablissement;

    /** Contestation par le bailleur (une seule par dépense) : signalée à l'agence, qui répond ; rien n'est annulé automatiquement. */
    @Column(name = "motif_contestation")
    private String motifContestation;

    @Column(name = "date_contestation")
    private java.time.LocalDateTime dateContestation;

    @Column(name = "reponse_contestation")
    private String reponseContestation;

    @Column(name = "date_reponse_contestation")
    private java.time.LocalDateTime dateReponseContestation;

    @Column(name = "user_reponse_contestation")
    private Integer userReponseContestation;

    /** Dépense au-dessus du plafond du mandat : l'agent atteste l'accord du bailleur. */
    @Column(name = "accord_bailleur", nullable = false)
    @Builder.Default
    private Boolean accordBailleur = false;

    /** Qui a donné l'accord, quand et comment, quand il est attesté par l'agent (jamais pour un devis accepté sur la plateforme). */
    @Column(name = "accord_motif")
    private String accordMotif;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutDepense statut = StatutDepense.EN_COURS;

    /** Devis accepté par le bailleur dont cette dépense est issue ; null = dépense saisie directement. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_devis")
    private DevisTravaux devis;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @Column(name = "user_create")
    private Integer userCreate;

    @Column(name = "user_update")
    private Integer userUpdate;
}
