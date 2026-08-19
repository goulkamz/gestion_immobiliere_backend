package com.immobilier.gestionImmobiliere.donnees.biens.model;

import com.immobilier.gestionImmobiliere.donnees.Model_1;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.List;

@Entity
@Table(name = "categorie_bien_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE categorie_bien_service SET is_deleted = true WHERE id_categorie = ?")
@Where(clause = "is_deleted = false")
public class CategorieBienService extends Model_1 {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_categorie")
    @SequenceGenerator(name = "seq_categorie", sequenceName = "seq_categorie", allocationSize = 1)
    @Column(name = "id_categorie")
    private Integer idCategorie;

    @Column(name = "libelle")
    private String libelle;

    @Column(name = "description")
    private String description;

    @Column(name = "user_create")
    private Integer userCreate;

    @Column(name = "user_update")
    private Integer userUpdate;


    @OneToMany(mappedBy = "categorie", fetch = FetchType.LAZY)
    private List<BienService> biensServices;
}