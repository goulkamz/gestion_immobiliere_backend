package com.immobilier.gestionImmobiliere.donnees.localisation.repository;

import com.immobilier.gestionImmobiliere.donnees.localisation.model.Secteur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecteurRepository extends JpaRepository<Secteur, Integer> {
    Page<Secteur> findByVille_IdVille(Integer idVille, Pageable pageable);
    // Recherche insensible a la casse sur le nom, le code ou la ville ; `motif` est deja au format LIKE ("%texte%", en minuscules)
    @Query("""
            SELECT s FROM Secteur s
            WHERE (:idVille IS NULL OR s.ville.idVille = :idVille)
              AND (LOWER(s.nomSecteur) LIKE :motif
                   OR LOWER(s.codeSecteur) LIKE :motif
                   OR LOWER(s.ville.nomVille) LIKE :motif)
            """)
    Page<Secteur> rechercher(@Param("idVille") Integer idVille, @Param("motif") String motif, Pageable pageable);

    boolean existsByCodeSecteurIgnoreCaseAndVille_IdVille(String codeSecteur, Integer idVille);

   // Statistiques

    @Query("SELECT COUNT(DISTINCT s.ville.idVille) FROM Secteur s WHERE s.isDeleted = false")
    long countVillesCouvertes();

    long countByIsDeletedFalse();
}