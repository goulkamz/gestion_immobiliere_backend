package com.immobilier.gestionImmobiliere.donnees.annonces.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Entity
@Table(name = "annonce")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE annonce SET is_deleted = true WHERE id_annonce = ?")
@Where(clause = "is_deleted = false")
public class Annonce extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_annonce")
    private Integer idAnnonce;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "description")
    private String description;

    @Column(name = "type_annonce")
    private String typeAnnonce;

    @Column(name = "date_publication")
    private LocalDate datePublication;

    @Column(name = "date_expiration", nullable = false)
    private LocalDate dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutAnnonce statut;

    @Column(name = "prix")
    private Double prix;

    @Column(name = "localisation")
    private String localisation;
}