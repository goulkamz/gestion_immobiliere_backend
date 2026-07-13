package com.immobilier.gestionImmobiliere.donnees.localisation.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Table(name = "pays")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLDelete(sql = "UPDATE localisation SET is_deleted = true WHERE id_pays = ?")
@Where(clause = "is_deleted = false")
public class Pays {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pays")
    @SequenceGenerator(name = "seq_pays", sequenceName = "seq_pays", allocationSize = 1)
    @Column(name = "id_pays")
    private Integer idPays;

    @Column(name = "code_pays", nullable = false, unique = true)
    private String codePays;

    @Column(name = "nom_pays", nullable = false)
    private String nomPays;

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