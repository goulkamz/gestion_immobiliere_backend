package com.immobilier.gestionImmobiliere.donnees.biens.repository;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MaisonRepository extends JpaRepository<Maison, Integer> {
    Page<Maison> findByCour_IdCour(Integer idCour, Pageable pageable);
    Page<Maison> findByStatut(StatutMaison statut, Pageable pageable);
    @Query("SELECT COALESCE(SUM(m.loyer), 0) FROM Maison m WHERE m.cour.idCour = :idCour AND m.statut = 'LOUEE'")
    Double sumLoyerMaisonsLoueesByCour(@Param("idCour") Integer idCour);

    // Statistiques maisonRepository

    @Query("SELECT m.statut, COUNT(m) FROM Maison m WHERE m.isDeleted = false GROUP BY m.statut")
    List<Object[]> countByStatut();

    @Query(value = "SELECT ROUND((COUNT(*) FILTER (WHERE statut = 'LOUEE')::NUMERIC / NULLIF(COUNT(*),0)) * 100, 2) FROM maison WHERE is_deleted = false", nativeQuery = true)
    Double tauxOccupation();

    @Query(value = "SELECT v.nom_ville AS nomVille, COUNT(m.id_maison) AS nbMaisons " +
            "FROM maison m " +
            "JOIN cour c ON c.id_cour = m.id_cour " +
            "JOIN secteur s ON s.id_secteur = c.id_secteur " +
            "JOIN ville v ON v.id_ville = s.id_ville " +
            "WHERE m.is_deleted = false " +
            "GROUP BY v.nom_ville ORDER BY nbMaisons DESC", nativeQuery = true)
    List<Object[]> countMaisonsParVille();

}