package com.immobilier.gestionImmobiliere.donnees.parametres.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "parametre_systeme")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class ParametreSysteme extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametre")
    private Integer idParametre;

    @Column(name = "cle", nullable = false, unique = true)
    private String cle;

    @Column(name = "valeur", nullable = false)
    private String valeur;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_valeur", nullable = false)
    private TypeValeurParametre typeValeur;

    @Column(name = "description")
    private String description;

    @Column(name = "user_update")
    private Integer userUpdate;
}