package com.immobilier.gestionImmobiliere.donnees.depenses.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/** Part d'une dépense retenue sur un reversement (échéance MANDAT). */
@Entity
@Table(name = "depense_imputation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@SQLDelete(sql = "UPDATE depense_imputation SET is_deleted = true WHERE id_imputation = ?")
@SQLRestriction("is_deleted = false")
public class DepenseImputation extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imputation")
    private Integer idImputation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_depense", nullable = false)
    private DepenseBien depense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_echeance", nullable = false)
    private EcheanceLoyer echeance;

    @Column(name = "montant", nullable = false)
    private BigDecimal montant;
}
