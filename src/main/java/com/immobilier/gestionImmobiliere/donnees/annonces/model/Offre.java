package com.immobilier.gestionImmobiliere.donnees.annonces.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Entity
@Table(name = "offre")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE offre SET is_deleted = true WHERE id_offre = ?")
@Where(clause = "is_deleted = false")
public class Offre extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_offre")
    private Integer idOffre;

    @Column(name = "nom_complet", nullable = false)
    private String nomComplet;

    @Column(name = "email")
    private String email;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "type_offre")
    private String typeOffre;

    @Column(name = "titre")
    private String titre;

    @Column(name = "description")
    private String description;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "date_offre")
    private LocalDate dateOffre;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutOffre statut; // libre (pas de contrainte explicite dans le cahier des charges)

    @Column(name = "image")
    private Short image;
}