package com.immobilier.gestionImmobiliere.donnees.contrats.repository;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.Avenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AvenantRepository extends JpaRepository<Avenant, Integer> {

    List<Avenant> findByContratLocation_IdContratLocationOrderByNumeroOrdreAsc(Integer idContratLocation);

    List<Avenant> findByMandat_IdMandatOrderByNumeroOrdreAsc(Integer idMandat);

    @Query("SELECT COALESCE(MAX(a.numeroOrdre), 0) FROM Avenant a WHERE a.contratLocation.idContratLocation = :id")
    int dernierNumeroLocation(@Param("id") Integer idContratLocation);

    @Query("SELECT COALESCE(MAX(a.numeroOrdre), 0) FROM Avenant a WHERE a.mandat.idMandat = :id")
    int dernierNumeroMandat(@Param("id") Integer idMandat);
}
