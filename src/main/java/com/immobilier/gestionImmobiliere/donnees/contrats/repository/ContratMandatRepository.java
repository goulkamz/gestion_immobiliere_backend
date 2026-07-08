package com.immobilier.gestionImmobiliere.donnees.contrats.repository;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratMandatRepository extends JpaRepository<ContratMandat, Integer> {
    Page<ContratMandat> findByCour_IdCour(Integer idCour, Pageable pageable);
    Page<ContratMandat> findByStatut(StatutMandat statut, Pageable pageable);
    boolean existsByCour_IdCourAndStatut(Integer idCour, StatutMandat statut);
}