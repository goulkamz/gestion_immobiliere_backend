package com.immobilier.gestionImmobiliere.donnees.user.model;


import com.immobilier.gestionImmobiliere.donnees.Model;
import com.immobilier.gestionImmobiliere.donnees.roles.model.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

    @Entity
    @Table(name = "users")
    @Data
    @Builder
    @AllArgsConstructor
    public class User extends Model {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "iduser")
        private Integer idUser;

        @Column(name = "nom", length = 254)
        private String nom;

        @Column(name = "prenom", length = 254)
        private String prenom;

        @Column(name = "sexe", columnDefinition = "character(1)")
        private String sexe;

        @Column(name = "email",unique = true, length = 254)
        private String email;

        @NotBlank
        @Size(max = 120)
        @Column(name = "password")
        @JsonIgnore
        private String password;

        @Column(name = "datenaissance")
        @Temporal(TemporalType.DATE)
        private Date dateNaissance;

        @Column(name = "telephone",unique = true, length = 254)
        private String telephone;

        @Column(name = "telephone1",unique = true, length = 254)
        private String telephone1;

        @Column(name = "flagactif", columnDefinition = "character(1)")
        private String flagActif;

        @Column(name = "datecreate")
        @Temporal(TemporalType.DATE)
        private Date dateCreate;

        @Column(name = "datelastlogin")
        @Temporal(TemporalType.DATE)
        private Date dateLastLogin;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "idrole", nullable = false)
        private Role role;

        // Méthode utilitaire pour Spring Security (username = email)
        @JsonIgnore
        public String getUsername() {
            return email;
        }

        // Méthode utilitaire pour Spring Security (password à ajouter si nécessaire)
        @JsonIgnore
        public String getPassword() {
            return password;
        }

        // Vérifier si l'utilisateur est actif
        public boolean isActive() {
            return "1".equals(flagActif) || "O".equalsIgnoreCase(flagActif) || "Y".equalsIgnoreCase(flagActif);
        }

        public User (){};

}
