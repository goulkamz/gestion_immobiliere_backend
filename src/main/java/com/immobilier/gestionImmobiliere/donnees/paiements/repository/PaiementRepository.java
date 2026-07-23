package com.immobilier.gestionImmobiliere.donnees.paiements.repository;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.Paiement;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.PaiementEcheance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaiementRepository extends JpaRepository<Paiement, Integer> {
}