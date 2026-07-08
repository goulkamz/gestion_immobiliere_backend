package com.immobilier.gestionImmobiliere.donnees.localisation.repository;

import com.immobilier.gestionImmobiliere.donnees.localisation.model.Ville;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VilleRepository extends JpaRepository<Ville, Long> {
    Page<Ville> findByPays_IdPays(Long idPays, Pageable pageable);
    boolean existsByCodeVilleIgnoreCaseAndPays_IdPays(String codeVille, Long idPays);
}