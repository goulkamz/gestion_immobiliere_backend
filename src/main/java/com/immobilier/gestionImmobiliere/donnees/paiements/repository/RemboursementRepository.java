package com.immobilier.gestionImmobiliere.donnees.paiements.repository;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.Remboursement;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEntiteRemboursement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RemboursementRepository extends JpaRepository<Remboursement, Integer> {

    List<Remboursement> findByEntiteTypeAndEntiteIdAndIsDeletedFalse(TypeEntiteRemboursement entiteType, Integer entiteId);
}