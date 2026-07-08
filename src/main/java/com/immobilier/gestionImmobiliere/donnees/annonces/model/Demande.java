package com.immobilier.gestionImmobiliere.donnees.annonces.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Entity
@Table(name = "demande")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE demande SET is_deleted = true WHERE id_demande = ?")
@Where(clause = "is_deleted = false")
public class Demande extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande")
    private Integer idDemande;

    @Column(name = "nom_complet", nullable = false)
    private String nomComplet;

    @Column(name = "email")
    private String email;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "type_bien")
    private String typeBien;

    @Column(name = "localisation_souhaite")
    private String localisationSouhaite;

    @Column(name = "budget_max")
    private Double budgetMax;

    @Column(name = "description")
    private String description;

    @Column(name = "date_demande")
    private LocalDate dateDemande;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutDemande statut;
}