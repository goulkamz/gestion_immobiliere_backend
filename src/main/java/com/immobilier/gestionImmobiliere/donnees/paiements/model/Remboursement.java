package com.immobilier.gestionImmobiliere.donnees.paiements.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import com.immobilier.gestionImmobiliere.donnees.biens.model.LocationBienService;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "remboursement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Remboursement extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_remboursement")
    @SequenceGenerator(name = "seq_remboursement", sequenceName = "seq_remboursement", allocationSize = 1)
    @Column(name = "id_remboursement")
    private Integer idRemboursement;

    @Enumerated(EnumType.STRING)
    @Column(name = "entite_type", nullable = false)
    private TypeEntiteRemboursement entiteType;

    @Column(name = "entite_id", nullable = false)
    private Integer entiteId;

    @Column(name = "montant", nullable = false)
    private Double montant;

    @Column(name = "mode_remboursement")
    private String modeRemboursement;

    @Column(name = "reference")
    private String reference;

    @Column(name = "motif")
    private String motif;

    @Column(name = "user_create")
    private Integer userCreate;

    @Column(name = "user_update")
    private Integer userUpdate;

}