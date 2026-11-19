package com.immobilier.gestionImmobiliere.donnees.depenses.repository;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseReglement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepenseReglementRepository extends JpaRepository<DepenseReglement, Integer> {

    List<DepenseReglement> findByDepense_IdDepenseOrderByDateReglementAscIdReglementAsc(Integer idDepense);

    /** Règlements d'une dépense qui comptent encore (hors règlements annulés), du plus ancien au plus récent. */
    List<DepenseReglement> findByDepense_IdDepenseAndAnnuleFalseOrderByIdReglementAsc(Integer idDepense);
}
