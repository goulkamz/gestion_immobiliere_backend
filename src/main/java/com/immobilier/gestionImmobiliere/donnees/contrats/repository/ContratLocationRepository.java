package com.immobilier.gestionImmobiliere.donnees.contrats.repository;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContratLocationRepository extends JpaRepository<ContratLocation, Integer> {
    Page<ContratLocation> findByLocataire_IdUser(Integer idUser, Pageable pageable);
    Page<ContratLocation> findByMaison_IdMaison(Integer idMaison, Pageable pageable);
    boolean existsByMaison_IdMaisonAndStatut(Integer idMaison, StatutLocation statut);

    List<ContratLocation> findByStatut(StatutLocation statut);

    Page<ContratLocation> findByMaison_Cour_Proprietaire_IdUser(Integer idUser, Pageable pageable);

    @Query("SELECT cl.idContratLocation FROM ContratLocation cl WHERE cl.locataire.idUser = :idUser")
    List<Integer> findIdsByLocataire(@Param("idUser") Integer idUser);

    @Query("SELECT cl.idContratLocation FROM ContratLocation cl WHERE cl.maison.cour.proprietaire.idUser = :idUser")
    List<Integer> findIdsByProprietaire(@Param("idUser") Integer idUser);

    // Statistiques contratLocationRepository

    @Query("SELECT c.statut, COUNT(c) FROM ContratLocation c WHERE c.isDeleted = false GROUP BY c.statut")
    List<Object[]> countByStatut();

    @Query(value = "SELECT v.nom_ville AS nomVille, COUNT(DISTINCT cl.id_user) AS nbLocataires " +
            "FROM contra_location cl " +
            "JOIN maison m ON m.id_maison = cl.id_maison " +
            "JOIN cour c ON c.id_cour = m.id_cour " +
            "JOIN secteur s ON s.id_secteur = c.id_secteur " +
            "JOIN ville v ON v.id_ville = s.id_ville " +
            "WHERE cl.statut = 'ACTIF' AND cl.is_deleted = false " +
            "GROUP BY v.nom_ville ORDER BY nbLocataires DESC", nativeQuery = true)
    List<Object[]> countLocatairesActifsParVille();
}