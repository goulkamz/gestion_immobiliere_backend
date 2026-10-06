package com.immobilier.gestionImmobiliere.donnees.annonces.model;

import com.immobilier.gestionImmobiliere.donnees.Model_1;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "temoignage")
@Data @SuperBuilder
@NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE temoignage SET is_deleted = true WHERE id_temoignage = ?")
@SQLRestriction("is_deleted = false")
public class Temoignage extends Model_1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_temoignage")
    private Integer idTemoignage;

    @Column(name = "nom_auteur", nullable = false)
    private String nomAuteur;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private RoleTemoignage role;

    @Column(name = "texte", nullable = false)
    private String texte;

    @Column(name = "note")
    private Short note;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "date_temoignage", nullable = false)
    private LocalDate dateTemoignage;

    // false = retire de l'affichage public par un admin
    @Column(name = "flag_actif", nullable = false)
    @Builder.Default
    private boolean flagActif = true;
}
