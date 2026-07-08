package com.immobilier.gestionImmobiliere.donnees.biens.repository;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaisonRepository extends JpaRepository<Maison, Integer> {
    Page<Maison> findByCour_IdCour(Integer idCour, Pageable pageable);
    Page<Maison> findByStatut(StatutMaison statut, Pageable pageable);
}