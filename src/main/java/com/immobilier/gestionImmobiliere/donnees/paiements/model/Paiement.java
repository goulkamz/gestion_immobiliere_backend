package com.immobilier.gestionImmobiliere.donnees.paiements.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "paiement")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE paiement SET is_deleted = true WHERE id_paiement = ?")
@Where(clause = "is_deleted = false")
public class Paiement extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paiement")
    private Integer idPaiement;

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Column(name = "montant_paiement")
    private Double montantPaiement;

    @Column(name = "mode_paiement")
    private String modePaiement;

    @Column(name = "reference_paiement")
    private String referencePaiement;

    @Column(name = "user_create")
    private Integer userCreate;
}