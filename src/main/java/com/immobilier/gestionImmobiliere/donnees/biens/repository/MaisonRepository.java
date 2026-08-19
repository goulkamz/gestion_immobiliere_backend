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

    // Maisons par statut
    @Query("SELECT m.statut, COUNT(m) FROM Maison m WHERE m.isDeleted = false GROUP BY m.statut")
    List<Object[]> countByStatut();

    // Taux d'occupation global
    @Query(value = "SELECT ROUND((COUNT(*) FILTER (WHERE statut = 'Louée')::NUMERIC " +
            "/ NULLIF(COUNT(*), 0)) * 100, 2) FROM Maison WHERE is_deleted = false", nativeQuery = true)
    BigDecimal tauxOccupation();

    // Nombre de maisons disponibles (public)
    long countByIsDeletedFalseAndStatut(String statut);
}