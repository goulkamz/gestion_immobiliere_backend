package com.immobilier.gestionImmobiliere.donnees.annonces.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Entity
@Table(name = "contact")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE contact SET is_deleted = true WHERE id_contact = ?")
@Where(clause = "is_deleted = false")
public class Contact extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contact")
    private Integer idContact;

    @Column(name = "nom_complet", nullable = false)
    private String nomComplet;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "sujet")
    private String sujet;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "date_envoi")
    private LocalDate dateEnvoi;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutContact statut;
}