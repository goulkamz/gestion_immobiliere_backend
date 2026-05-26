package com.immobilier.gestionImmobiliere.donnees.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Entity
    @Table(name = "validation")
    public class Validation {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id_validation")
        private Integer id;

        @Column(name = "creation", nullable = false)
        private Instant creation;

        @Column(name = "expiration", nullable = false)
        private Instant expiration;

        @Column(name = "code", length = 255, nullable = false)
        private String code;

        @OneToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "id_user", referencedColumnName = "id_user")
        private User user;

}
