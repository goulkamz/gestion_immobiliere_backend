package com.immobilier.gestionImmobiliere.donnees.user.repository;

import com.immobilier.gestionImmobiliere.donnees.user.model.CodeSecours2fa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CodeSecours2faRepository extends JpaRepository<CodeSecours2fa, Long> {

    long countByIdUserAndUtiliseLeIsNull(Integer idUser);

    void deleteByIdUser(Integer idUser);

    /** Consommation atomique : retourne 1 si le code existait et n'etait pas deja utilise. */
    @Modifying
    @Query("UPDATE CodeSecours2fa c SET c.utiliseLe = :maintenant "
            + "WHERE c.idUser = :idUser AND c.codeHash = :hash AND c.utiliseLe IS NULL")
    int consommer(@Param("idUser") Integer idUser, @Param("hash") String hash,
                  @Param("maintenant") LocalDateTime maintenant);
}
