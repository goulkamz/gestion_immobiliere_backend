package com.immobilier.gestionImmobiliere.donnees.documents.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Document extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_document")
    private Integer idDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false)
    private TypeDocument typeDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "entite_type")
    private TypeEntiteDocument entiteType;

    @Column(name = "entite_id")
    private Integer entiteId;

    @Column(name = "periode_mois")
    private LocalDate periodeMois;

    @Column(name = "chemin_fichier", nullable = false)
    private String cheminFichier;

    @Column(name = "user_create")
    private Integer userCreate;
}
