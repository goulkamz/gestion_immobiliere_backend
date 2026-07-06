package com.immobilier.gestionImmobiliere.donnees.secteur.repository;

import com.immobilier.gestionImmobiliere.donnees.secteur.model.Secteur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecteurRepository extends JpaRepository<Secteur, Long> {
    Page<Secteur> findByVille_IdVille(Long idVille, Pageable pageable);
    boolean existsByCodeSecteurIgnoreCaseAndVille_IdVille(String codeSecteur, Long idVille);
}