package com.immobilier.gestionImmobiliere.donnees.paiements.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Entity
@Table(name = "paiement_echeance")
@Data @SuperBuilder
@NoArgsConstructor @AllArgsConstructor
@IdClass(PaiementEcheance.PaiementEcheanceId.class)
public class PaiementEcheance {

    @Id
    @Column(name = "id_echeance")
    private Integer idEcheance;

    @Id
    @Column(name = "id_paiement")
    private Integer idPaiement;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PaiementEcheanceId implements Serializable {
        private Integer idEcheance;
        private Integer idPaiement;
    }
}