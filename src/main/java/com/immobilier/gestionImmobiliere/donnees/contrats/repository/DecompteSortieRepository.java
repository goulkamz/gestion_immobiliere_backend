package com.immobilier.gestionImmobiliere.donnees.contrats.repository;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.DecompteSortie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecompteSortieRepository extends JpaRepository<DecompteSortie, Integer> {
    java.util.Optional<DecompteSortie> findByContratLocation_IdContratLocation(Integer idContratLocation);
}