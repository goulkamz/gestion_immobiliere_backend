package com.immobilier.gestionImmobiliere.donnees.biens.repository;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Cour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourRepository extends JpaRepository<Cour, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Cour> {
    Page<Cour> findBySecteur_IdSecteur(Integer idSecteur, Pageable pageable);
    Page<Cour> findByProprietaire_IdUser(Integer idUser, Pageable pageable);
    Page<Cour> findBySecteur_IdSecteurAndProprietaire_IdUser(Integer idSecteur, Integer idUser, Pageable pageable);
    long countByIsDeletedFalse();

    // Statistiques courRepository

    @Query(value = "SELECT u.id_user AS idBailleur, u.nom || ' ' || u.prenom AS nomComplet, COUNT(m.id_maison) AS nbMaisons, " +
            "COALESCE((SELECT SUM(COALESCE(e.montant_paye, 0)) FROM echeance_loyer e " +
            "          JOIN contra_location cl ON cl.id_contra_location = e.entite_echeance_id " +
            "          JOIN maison m2 ON m2.id_maison = cl.id_maison " +
            "          JOIN cour c2 ON c2.id_cour = m2.id_cour " +
            "          WHERE e.entite_echeance_type = 'LOCATION' AND e.is_deleted = false AND c2.id_user = u.id_user), 0) AS loyersEncaisses " +
            "FROM maison m " +
            "JOIN cour c ON c.id_cour = m.id_cour " +
            "JOIN users u ON u.id_user = c.id_user " +
            "WHERE m.is_deleted = false " +
            "GROUP BY u.id_user, u.nom, u.prenom " +
            "ORDER BY nbMaisons DESC, loyersEncaisses DESC LIMIT :limite", nativeQuery = true)
    List<Object[]> topBailleursParNbMaisons(@Param("limite") int limite);

    long countByProprietaire_IdUserAndIsDeletedFalse(Integer idBailleur);
}