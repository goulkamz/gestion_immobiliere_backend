package com.immobilier.gestionImmobiliere.donnees.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.time.Instant;

@Entity
@Table(name = "pending_registration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false,name = "mot_de_passe")
    private String password;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private Character  sexe;

    private String telephone;

    private String telephone1;

    @Column(name = "id_role")
    private Integer idRole;
    @Column(name = "flag_actif")
    private boolean flagActif;

    @Column(name = "date_naissance")
    @Temporal(TemporalType.DATE)
    private Date dateNaissance;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private Instant creation;

    @Column(nullable = false)
    private Instant expiration;

    @Column(name = "tentative_envoi")
    private Integer tentativeEnvoi = 0;
}