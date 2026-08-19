package com.immobilier.gestionImmobiliere.donnees.paiements.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import com.immobilier.gestionImmobiliere.donnees.biens.model.LocationBienService;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;


@Entity
@Table(name = "paiement_location_bien_service")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@IdClass(PaiementLocationBienService.PaiementLocationId.class)
public class PaiementLocationBienService extends Model {

    @Id
    @Column(name = "id_location_bien_service")
    private Integer idLocationBienService;

    @Id
    @Column(name = "id_paiement")
    private Integer idPaiement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_location_bien_service", insertable = false, updatable = false)
    private LocationBienService location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paiement", insertable = false, updatable = false)
    private Paiement paiement;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_paiement", nullable = false)
    @Builder.Default
    private TypePaiementLocationBienService typePaiement = TypePaiementLocationBienService.INITIAL;

    // Classe imbriquée manquante : représente la clé primaire composite
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaiementLocationId implements Serializable {
        private Integer idLocationBienService;
        private Integer idPaiement;
    }
}