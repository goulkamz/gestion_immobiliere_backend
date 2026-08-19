package com.immobilier.gestionImmobiliere.donnees.biens.repository;

import com.immobilier.gestionImmobiliere.donnees.biens.model.CategorieBienService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategorieBienServiceRepository extends JpaRepository<CategorieBienService,Integer> {
    // Nombre de catégories actives (pour un éventuel écran d'admin)
    long countByIsDeletedFalse();
}
