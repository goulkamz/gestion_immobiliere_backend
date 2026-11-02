package com.immobilier.gestionImmobiliere.donnees.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Code de secours de la double authentification : stocke hache, a usage unique. */
@Entity
@Table(name = "codes_secours_2fa")
@Getter
@Setter
@NoArgsConstructor
public class CodeSecours2fa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_code")
    private Long idCode;

    @Column(name = "id_user", nullable = false)
    private Integer idUser;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "utilise_le")
    private LocalDateTime utiliseLe;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CodeSecours2fa(Integer idUser, String codeHash) {
        this.idUser = idUser;
        this.codeHash = codeHash;
    }
}
