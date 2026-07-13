package com.immobilier.gestionImmobiliere.donnees.localisation.repository;

import com.immobilier.gestionImmobiliere.donnees.localisation.model.Secteur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecteurRepository extends JpaRepository<Secteur, Integer> {
    Page<Secteur> findByVille_IdVille(Integer idVille, Pageable pageable);
    boolean existsByCodeSecteurIgnoreCaseAndVille_IdVille(String codeSecteur, Integer idVille);
}