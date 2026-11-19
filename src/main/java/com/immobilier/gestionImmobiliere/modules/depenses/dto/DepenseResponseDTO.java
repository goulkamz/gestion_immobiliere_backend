package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.CategorieDepense;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDepense;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder
public class DepenseResponseDTO {
    private Integer idDepense;
    private Integer idMandat;
    private Integer idCour;
    private String referenceCour;
    private Integer idMaison;
    private String nomMaison;
    private String nomProprietaire;
    private String libelle;
    private CategorieDepense categorie;
    private String description;
    private String prestataire;
    private LocalDate dateDepense;
    private BigDecimal montant;
    private Integer nombreMois;
    /** Retenue visée à chaque reversement (solde à étaler / nombre de mois de l'étalement en cours). */
    private BigDecimal mensualite;
    /** Reversements encore prévus pour solder la dépense (null si elle n'est plus en cours). */
    private Integer moisRestants;
    private BigDecimal montantImpute;
    /** Réglé directement par le bailleur, hors reversement (mandat résilié). */
    private BigDecimal montantRegle;
    private BigDecimal montantRestant;
    /** Étalement d'origine si la dépense a été rééchelonnée (null sinon). */
    private Integer nombreMoisInitial;
    private String motifReechelonnement;
    private java.time.LocalDateTime dateReechelonnement;
    private Boolean accordBailleur;
    /** Accord attesté par l'agent : qui, quand, comment (null si l'accord vient d'un devis accepté sur la plateforme). */
    private String accordMotif;
    /** Reste dû abandonné par l'agence (abandon de créance), 0 sinon. */
    private BigDecimal montantAbandonne;
    private String motifAbandon;
    private java.time.LocalDateTime dateAbandon;
    /** Créance abandonnée puis rétablie par l'admin (trace). */
    private String motifRetablissement;
    private java.time.LocalDateTime dateRetablissement;
    /** Contestation du bailleur : en attente de réponse tant que {@code dateReponseContestation} est nul. */
    private String motifContestation;
    private java.time.LocalDateTime dateContestation;
    private String reponseContestation;
    private java.time.LocalDateTime dateReponseContestation;
    /** Contestation ouverte, sans réponse de l'agence. */
    private Boolean contestationOuverte;
    /** Mandat toujours actif mais dont la date de fin est dépassée, alors que la dépense est en cours de retenue. */
    private Boolean mandatEchu;
    private java.time.LocalDateTime dateFinMandat;
    /** Devis accepté par le bailleur dont la dépense est issue (null = saisie directe). */
    private Integer idDevis;
    private StatutDepense statut;
    private String motifAnnulation;
    /** Renseigné uniquement dans le détail d'une dépense. */
    private List<DepenseImputationResponseDTO> imputations;
    /** Règlements directs du bailleur ; renseigné uniquement dans le détail d'une dépense. */
    private List<DepenseReglementResponseDTO> reglements;
}
