package com.immobilier.gestionImmobiliere.donnees.contrats.repository;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContratMandatRepository extends JpaRepository<ContratMandat, Integer> {
    Page<ContratMandat> findByCour_IdCour(Integer idCour, Pageable pageable);
    Page<ContratMandat> findByStatut(StatutMandat statut, Pageable pageable);
    boolean existsByCour_IdCourAndStatut(Integer idCour, StatutMandat statut);
    Page<ContratMandat> findByCour_Proprietaire_IdUser(Integer idProprietaire, Pageable pageable);

    List<ContratMandat> findByStatut(StatutMandat statut);

    @Query("SELECT m.idMandat FROM ContratMandat m WHERE m.cour.proprietaire.idUser = :idUser")
    List<Integer> findIdsByProprietaire(@Param("idUser") Integer idUser);

    //Statistiques contratMandatRepository

    @Query("SELECT m.statut, COUNT(m) FROM ContratMandat m WHERE m.isDeleted = false GROUP BY m.statut")
    List<Object[]> countByStatut();

    @Query(value = "SELECT COUNT(*) FROM contrat_mandat WHERE is_deleted = false AND statut = 'ACTIF' " +
            "AND date_fin BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '30 days'", nativeQuery = true)
    long countExpirantSous30Jours();

    List<ContratMandat> findByAgent_IdUser(Integer idAgent);


    @Query("SELECT m FROM ContratMandat m WHERE m.cour.proprietaire.idUser = :idBailleur AND m.statut = com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat.ACTIF AND m.isDeleted = false")
    Optional<ContratMandat> findMandatActifPourBailleur(@Param("idBailleur") Integer idBailleur);
}