package com.immobilier.gestionImmobiliere.donnees.depenses.repository;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.DevisTravaux;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDevis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface DevisTravauxRepository extends JpaRepository<DevisTravaux, Integer>, JpaSpecificationExecutor<DevisTravaux> {

    /** Devis dans un statut donné dont la soumission en cours date d'avant {@code seuil} (expiration, rappel). */
    List<DevisTravaux> findByStatutAndDateSoumissionBefore(StatutDevis statut, LocalDateTime seuil);

    /** Devis dans un statut donné dont la réponse du bailleur date d'avant {@code seuil} (rappel des devis acceptés non réalisés). */
    List<DevisTravaux> findByStatutAndDateReponseBefore(StatutDevis statut, LocalDateTime seuil);

    List<DevisTravaux> findByMandat_IdMandatAndStatutIn(Integer idMandat, List<StatutDevis> statuts);

    long countByCour_Proprietaire_IdUserAndStatut(Integer idUser, StatutDevis statut);

    long countByStatut(StatutDevis statut);
}
