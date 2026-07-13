package com.immobilier.gestionImmobiliere.donnees.biens.repository;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Cour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourRepository extends JpaRepository<Cour, Integer> {
    Page<Cour> findBySecteur_IdSecteur(Integer idSecteur, Pageable pageable);
    Page<Cour> findByProprietaire_IdUser(Integer idUser, Pageable pageable);
}