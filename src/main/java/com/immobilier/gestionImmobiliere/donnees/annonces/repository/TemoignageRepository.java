package com.immobilier.gestionImmobiliere.donnees.annonces.repository;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutTemoignage;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.Temoignage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TemoignageRepository extends JpaRepository<Temoignage, Integer>, JpaSpecificationExecutor<Temoignage> {

    Page<Temoignage> findByStatut(StatutTemoignage statut, Pageable pageable);

    List<Temoignage> findByStatutOrderByDateTemoignageDescIdTemoignageDesc(StatutTemoignage statut);

    // Suppression definitive : la requete HQL contourne le @SQLDelete (suppression logique) de l'entite
    @Modifying
    @Query("delete from Temoignage t where t.idTemoignage = :id")
    void supprimerDefinitivement(@Param("id") Integer id);
}
