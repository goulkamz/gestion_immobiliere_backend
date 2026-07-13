package com.immobilier.gestionImmobiliere.donnees.contrats.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import com.immobilier.gestionImmobiliere.donnees.biens.model.Cour;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contrat_mandat")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE contrat_mandat SET is_deleted = true WHERE id_mandat = ?")
@Where(clause = "is_deleted = false")
public class ContratMandat extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mandat")
    private Integer idMandat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cour", nullable = false)
    private Cour cour;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User agent;

    @Column(name = "date_debut")
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_mandat")
    private TypeMandat typeMandat;

    @Column(name = "commission")
    private BigDecimal commission;

    @Column(name = "mode_facturation")
    private String modeFacturation;

    @Column(name = "date_resiliation")
    private LocalDateTime dateResiliation;

    @Column(name = "motif_resiliation")
    private String motifResiliation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutMandat statut;
}