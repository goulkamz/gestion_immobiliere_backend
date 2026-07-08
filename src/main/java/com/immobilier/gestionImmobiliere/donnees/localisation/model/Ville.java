package com.immobilier.gestionImmobiliere.donnees.localisation.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Table(name = "ville")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLDelete(sql = "UPDATE ville SET is_deleted = true WHERE id_ville = ?")
@Where(clause = "is_deleted = false")
public class Ville {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_ville")
    @SequenceGenerator(name = "seq_ville", sequenceName = "seq_ville", allocationSize = 1)
    @Column(name = "id_ville")
    private Long idVille;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pays", nullable = false)
    private Pays pays;

    @Column(name = "code_ville", nullable = false)
    private String codeVille;

    @Column(name = "nom_ville", nullable = false)
    private String nomVille;

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