package com.immobilier.gestionImmobiliere.donnees.contrats.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Entity
@Table(name = "contra_location")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE contra_location SET is_deleted = true WHERE id_contra_location = ?")
@Where(clause = "is_deleted = false")
public class ContratLocation extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contra_location")
    private Integer idContratLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User locataire;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_maison", nullable = false)
    private Maison maison;

    @Column(name = "date_entree")
    private LocalDate dateEntree;

    @Column(name = "date_sortie")
    private LocalDate dateSortie;

    @Column(name = "montant_loyer")
    private Double montantLoyer;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutLocation statut;

    @Column(name = "type_contrat")
    private String typeContrat;

    @Column(name = "etat_des_lieux_entree")
    private String etatDesLieuxEntree;

    @Column(name = "etat_des_lieux_sortie")
    private String etatDesLieuxSortie;

    @Column(name = "user_create")
    private Integer userCreate;

    @Column(name = "user_update")
    private Integer userUpdate;

    @Column(name = "date_create")
    private LocalDate dateCreate;

    @Column(name = "date_update")
    private LocalDate dateUpdate;
}