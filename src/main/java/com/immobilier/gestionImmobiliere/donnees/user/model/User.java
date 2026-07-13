package com.immobilier.gestionImmobiliere.donnees.user.model;


import com.immobilier.gestionImmobiliere.donnees.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

    @Entity
    @Table(name = "users")
    @Data
    @Builder
    @AllArgsConstructor
    public class User extends Model {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id_user")
        private Integer idUser;

        @Column(name = "nom", length = 254)
        private String nom;

        @Column(name = "prenom", length = 254)
        private String prenom;

        @Column(name = "sexe")
        private Character sexe;

        @Column(name = "email",unique = true, length = 254)
        private String email;

        @NotBlank
        @Size(max = 120)
        @Column(name = "mot_de_passe")
        @JsonIgnore
        private String password;

        @Column(name = "date_naissance")
        private LocalDate dateNaissance;

        @Column(name = "telephone",unique = true, length = 254)
        private String telephone;

        @Column(name = "telephone1",unique = true, length = 254)
        private String telephone1;

        @Column(name = "flag_actif")
        private boolean flagActif;

        @Column(name = "date_create")
        private LocalDateTime dateCreate;

        @Column(name = "date_last_login")
        private LocalDateTime dateLastLogin;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "id_role", nullable = false)
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
            return flagActif;
        }

        public User (){};

    }
