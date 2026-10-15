package com.immobilier.gestionImmobiliere.donnees.contrats.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Avenant a un contrat actif (location OU mandat) : trace d'une modification acceptee par le client.
 * Le contrat d'origine reste fige ; l'avenant liste ce qui change, a partir de {@code dateEffet}.
 */
@Entity
@Table(name = "avenant")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@SQLRestriction("is_deleted = false")
public class Avenant extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_avenant")
    private Integer idAvenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrat_location")
    private ContratLocation contratLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mandat")
    private ContratMandat mandat;

    @Column(name = "numero_ordre", nullable = false)
    private Integer numeroOrdre;

    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    @Column(name = "motif", nullable = false)
    private String motif;

    @Column(name = "user_create")
    private Integer userCreate;

    @OneToMany(mappedBy = "avenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idLigne ASC")
    @Builder.Default
    private List<AvenantLigne> lignes = new ArrayList<>();
}
