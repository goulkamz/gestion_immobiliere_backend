package com.immobilier.gestionImmobiliere.donnees.biens.repository;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Cour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourRepository extends JpaRepository<Cour, Integer> {
    Page<Cour> findBySecteur_IdSecteur(Integer idSecteur, Pageable pageable);
    Page<Cour> findByProprietaire_IdUser(Integer idUser, Pageable pageable);
    Page<Cour> findBySecteur_IdSecteurAndProprietaire_IdUser(Integer idSecteur, Integer idUser, Pageable pageable);
    long countByIsDeletedFalse();

    // Statistiques courRepository

    @Query(value = "SELECT u.id_user AS idBailleur, u.nom || ' ' || u.prenom AS nomComplet, COUNT(m.id_maison) AS nbMaisons " +
            "FROM maison m " +
            "JOIN cour c ON c.id_cour = m.id_cour " +
            "JOIN users u ON u.id_user = c.id_user " +
            "WHERE m.is_deleted = false " +
            "GROUP BY u.id_user, u.nom, u.prenom " +
            "ORDER BY nbMaisons DESC LIMIT :limite", nativeQuery = true)
    List<Object[]> topBailleursParNbMaisons(@Param("limite") int limite);
}