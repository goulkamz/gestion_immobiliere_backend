package com.immobilier.gestionImmobiliere.donnees.paiements.repository;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.PaiementEcheance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaiementEcheanceRepository extends JpaRepository<PaiementEcheance, PaiementEcheance.PaiementEcheanceId> {
    List<PaiementEcheance> findByIdPaiement(Integer idPaiement);
}