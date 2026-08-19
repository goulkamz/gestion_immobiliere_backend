package com.immobilier.gestionImmobiliere.donnees.user.model;


import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "role")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role")
    private Integer idRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "libelle_role", length = 254)
    private ERole libelleRole;

    // Méthode utilitaire pour obtenir le nom du rôle (compatible Spring Security)
    public Role (ERole libelleRole) {
            this.libelleRole=libelleRole;
        }

}
