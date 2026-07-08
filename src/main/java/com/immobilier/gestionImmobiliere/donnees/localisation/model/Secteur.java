package com.immobilier.gestionImmobiliere.donnees.localisation.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Table(name = "secteur")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLDelete(sql = "UPDATE secteur SET is_deleted = true WHERE id_secteur = ?")
@Where(clause = "is_deleted = false")
public class Secteur {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_secteur")
    @SequenceGenerator(name = "seq_secteur", sequenceName = "seq_secteur", allocationSize = 1)
    @Column(name = "id_secteur")
    private Long idSecteur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ville", nullable = false)
    private Ville ville;

    @Column(name = "code_secteur", nullable = false)
    private String codeSecteur;

    @Column(name = "nom_secteur", nullable = false)
    private String nomSecteur;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}