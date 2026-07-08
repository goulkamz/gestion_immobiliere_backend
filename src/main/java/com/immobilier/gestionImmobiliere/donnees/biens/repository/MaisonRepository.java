package com.immobilier.gestionImmobiliere.donnees.biens.repository;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaisonRepository extends JpaRepository<Maison, Integer> {
    Page<Maison> findByCour_IdCour(Integer idCour, Pageable pageable);
    Page<Maison> findByStatut(StatutMaison statut, Pageable pageable);
    @Query("SELECT COALESCE(SUM(m.loyer), 0) FROM Maison m WHERE m.cour.idCour = :idCour AND m.statut = 'LOUEE'")
    Double sumLoyerMaisonsLoueesByCour(@Param("idCour") Integer idCour);
}