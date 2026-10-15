package com.immobilier.gestionImmobiliere.donnees.contrats.model;

import jakarta.persistence.*;
import lombok.*;

/** Un champ modifie par un avenant (valeurs conservees sous forme de texte). */
@Entity
@Table(name = "avenant_ligne")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AvenantLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ligne")
    private Integer idLigne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_avenant", nullable = false)
    private Avenant avenant;

    @Column(name = "champ", nullable = false)
    private String champ;

    @Column(name = "ancienne_valeur")
    private String ancienneValeur;

    @Column(name = "nouvelle_valeur")
    private String nouvelleValeur;
}
