package com.immobilier.gestionImmobiliere.donnees.biens.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Entity
@Table(name = "maison")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE maison SET is_deleted = true WHERE id_maison = ?")
@Where(clause = "is_deleted = false")
public class Maison extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_maison")
    private Integer idMaison;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cour", nullable = false)
    private Cour cour;

    @Column(name = "type_maison")
    private String typeMaison;

    @Column(name = "nom_commun_maison")
    private String nomCommunMaison;

    @Column(name = "nombre_piece")
    private Integer nombrePiece;

    @Column(name = "loyer")
    private Double loyer;

    @Column(name = "caution")
    private Double caution;

    @Column(name = "nombre_mois_caution")
    private Integer nombreMoisCaution;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutMaison statut;

    @Column(name = "user_create")
    private Integer userCreate;

    @Column(name = "user_update")
    private Integer userUpdate;

    @Column(name = "date_create")
    private LocalDate dateCreate;

    @Column(name = "date_update")
    private LocalDate dateUpdate;
}