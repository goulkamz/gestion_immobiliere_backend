package com.immobilier.gestionImmobiliere.donnees.paiements.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "echeance_loyer")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE echeance_loyer SET is_deleted = true WHERE id_echeance = ?")
@Where(clause = "is_deleted = false")
public class EcheanceLoyer extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_echeance")
    private Integer idEcheance;

    @Enumerated(EnumType.STRING)
    @Column(name = "entite_echeance_type", nullable = false)
    private TypeEcheance entiteEcheanceType;

    // Référence polymorphe -> id_mandat OU id_contra_location, résolue en code (pas de FK possible)
    @Column(name = "entite_echeance_id", nullable = false)
    private Integer entiteEcheanceId;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "montant_du")
    private Double montantDu;

    @Column(name = "montant_paye")
    private Double montantPaye;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutEcheance statut;

    @Column(name = "user_create")
    private Integer userCreate;

    @Column(name = "date_create")
    private LocalDateTime dateCreate;
}