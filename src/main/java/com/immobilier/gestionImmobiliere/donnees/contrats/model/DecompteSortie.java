package com.immobilier.gestionImmobiliere.donnees.contrats.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.Paiement;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "decompte_sortie")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@SQLDelete(sql = "UPDATE decompte_sortie SET is_deleted = true WHERE id_decompte = ?")
@Where(clause = "is_deleted = false")
public class DecompteSortie extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_decompte")
    private Integer idDecompte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrat_location", nullable = false)
    private ContratLocation contratLocation;

    @Column(name = "date_sortie", nullable = false)
    private LocalDateTime dateSortie;

    @Column(name = "montant_avance_reference", nullable = false)
    private BigDecimal montantAvanceReference;

    @Column(name = "montant_caution_reference", nullable = false)
    private BigDecimal montantCautionReference;

    @Column(name = "montant_arrieres", nullable = false)
    @Builder.Default
    private BigDecimal montantArrieres = BigDecimal.ZERO;

    @Column(name = "montant_deduit_avance", nullable = false)
    @Builder.Default
    private BigDecimal montantDeduitAvance = BigDecimal.ZERO;

    @Column(name = "cout_reparation", nullable = false)
    @Builder.Default
    private BigDecimal coutReparation = BigDecimal.ZERO;

    @Column(name = "montant_deduit_caution", nullable = false)
    @Builder.Default
    private BigDecimal montantDeduitCaution = BigDecimal.ZERO;

    @Column(name = "montant_manquant", nullable = false)
    @Builder.Default
    private BigDecimal montantManquant = BigDecimal.ZERO;

    @Column(name = "montant_a_rembourser", nullable = false)
    @Builder.Default
    private BigDecimal montantARembourser = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutDecompteSortie statut = StatutDecompteSortie.EN_ATTENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paiement")
    private Paiement paiement;

    @Column(name = "user_create")
    private Integer userCreate;

    @Column(name = "user_update")
    private Integer userUpdate;
}