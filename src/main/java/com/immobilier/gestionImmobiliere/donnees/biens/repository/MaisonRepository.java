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

public interface MaisonRepository extends JpaRepository<Maison, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Maison> {
    Page<Maison> findByCour_IdCour(Integer idCour, Pageable pageable);
    Page<Maison> findByStatut(StatutMaison statut, Pageable pageable);

    // Catalogue public : maison DISPONIBLE dont la cour a un mandat ACTIF de type GESTION ou LOCATION
    @Query("SELECT m FROM Maison m WHERE m.statut = com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison.DISPONIBLE " +
            "AND (:idCour IS NULL OR m.cour.idCour = :idCour) " +
            "AND (LOWER(m.nomCommunMaison) LIKE :motif OR LOWER(m.typeMaison) LIKE :motif " +
            "OR LOWER(m.cour.referenceCour) LIKE :motif) " +
            "AND EXISTS (SELECT 1 FROM ContratMandat c WHERE c.cour = m.cour " +
            "AND c.statut = com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat.ACTIF " +
            "AND c.typeMandat IN (com.immobilier.gestionImmobiliere.donnees.contrats.model.TypeMandat.GESTION, " +
            "com.immobilier.gestionImmobiliere.donnees.contrats.model.TypeMandat.LOCATION))")
    Page<Maison> findCataloguePublic(@Param("idCour") Integer idCour, @Param("motif") String motif, Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.loyer), 0) FROM Maison m WHERE m.cour.idCour = :idCour AND m.statut = 'LOUEE'")
    Double sumLoyerMaisonsLoueesByCour(@Param("idCour") Integer idCour);

    // Statistiques maisonRepository

    @Query("SELECT m.statut, COUNT(m) FROM Maison m WHERE m.isDeleted = false GROUP BY m.statut")
    List<Object[]> countByStatut();

    @Query(value = "SELECT ROUND((CAST(COUNT(*) FILTER (WHERE statut = 'LOUEE') AS NUMERIC) / NULLIF(COUNT(*),0)) * 100, 2) FROM maison WHERE is_deleted = false", nativeQuery = true)
    BigDecimal tauxOccupation();

    @Query(value = "SELECT v.nom_ville AS nomVille, COUNT(m.id_maison) AS nbMaisons " +
            "FROM maison m " +
            "JOIN cour c ON c.id_cour = m.id_cour " +
            "JOIN secteur s ON s.id_secteur = c.id_secteur " +
            "JOIN ville v ON v.id_ville = s.id_ville " +
            "WHERE m.is_deleted = false " +
            "GROUP BY v.nom_ville ORDER BY nbMaisons DESC", nativeQuery = true)
    List<Object[]> countMaisonsParVille();


    @Query("SELECT m.statut, COUNT(m) FROM Maison m WHERE m.cour.proprietaire.idUser = :idBailleur AND m.isDeleted = false GROUP BY m.statut")
    List<Object[]> countByStatutPourBailleur(@Param("idBailleur") Integer idBailleur);

    @Query(value = "SELECT ROUND((CAST(COUNT(*) FILTER (WHERE m.statut = 'LOUEE') AS NUMERIC) / NULLIF(COUNT(*),0)) * 100, 2) " +
            "FROM maison m JOIN cour c ON c.id_cour = m.id_cour " +
            "WHERE c.id_user = :idBailleur AND m.is_deleted = false", nativeQuery = true)
    Double tauxOccupationPourBailleur(@Param("idBailleur") Integer idBailleur);

    long countByIsDeletedFalseAndStatut(StatutMaison statut);

}