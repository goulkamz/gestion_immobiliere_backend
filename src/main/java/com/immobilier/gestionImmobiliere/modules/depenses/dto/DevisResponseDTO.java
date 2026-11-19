package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.CategorieDepense;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDevis;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class DevisResponseDTO {
    private Integer idDevis;
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
    private BigDecimal montant;
    private Integer nombreMois;
    /** Retenue visée à chaque reversement si le devis est accepté (montant / nombre de mois). */
    private BigDecimal mensualite;
    /** Plafond du mandat au-delà duquel l'accord du bailleur est nécessaire (null = aucun plafond). */
    private BigDecimal plafondDepenseSansAccord;
    private StatutDevis statut;
    private String commentaireBailleur;
    private LocalDateTime dateReponse;
    private Integer nombreSoumissions;
    private String motifAnnulation;
    private LocalDateTime dateSoumission;
    /** Date à laquelle le devis expire faute de réponse (uniquement tant qu'il est en attente). */
    private LocalDateTime dateLimiteReponse;
    /** Dépense enregistrée à partir du devis (statut REALISE). */
    private Integer idDepense;
    /** Accord donné hors plateforme et enregistré par l'agent (le bailleur peut le contester tant que la dépense n'existe pas). */
    private Boolean accordHorsPlateforme;
    private String accordMotif;
    private java.time.LocalDate accordDate;
    /** Un devis PDF de l'artisan est joint. */
    private Boolean pieceJointe;
}
