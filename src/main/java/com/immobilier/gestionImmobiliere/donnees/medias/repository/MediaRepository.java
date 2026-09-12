package com.immobilier.gestionImmobiliere.donnees.medias.repository;

import com.immobilier.gestionImmobiliere.donnees.medias.model.Media;
import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, Integer> {
    List<Media> findByEntiteTypeAndEntiteIdOrderByOrdreAsc(TypeEntiteMedia entiteType, Integer entiteId);
    Optional<Media> findByEntiteTypeAndEntiteIdAndIsPrincipalTrue(TypeEntiteMedia entiteType, Integer entiteId);
    long countByEntiteTypeAndEntiteId(TypeEntiteMedia entiteType, Integer entiteId);

    // MediaRepository — ajouter entite_type pour déterminer le bon bucket par ligne
    @Query(value = "SELECT id_media, media_path, media_path_thumbnail, entite_type FROM medias " +
            "WHERE is_deleted = true AND updated_at < :seuil", nativeQuery = true)
    List<Object[]> findCandidatsPurge(@Param("seuil") LocalDateTime seuil);

    @Modifying
    @Query(value = "DELETE FROM medias WHERE id_media = :id", nativeQuery = true)
    void hardDelete(@Param("id") Integer id);

    @Query(value = "SELECT media_path, media_path_thumbnail FROM medias", nativeQuery = true)
    List<Object[]> findAllCheminsIncluantSupprimes();
}