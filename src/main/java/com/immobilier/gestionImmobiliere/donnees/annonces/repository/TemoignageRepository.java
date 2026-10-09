package com.immobilier.gestionImmobiliere.donnees.annonces.repository;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutTemoignage;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.Temoignage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TemoignageRepository extends JpaRepository<Temoignage, Integer>, JpaSpecificationExecutor<Temoignage> {

    Page<Temoignage> findByStatut(StatutTemoignage statut, Pageable pageable);

    List<Temoignage> findByStatutOrderByDateTemoignageDescIdTemoignageDesc(StatutTemoignage statut);
}
