package com.immobilier.gestionImmobiliere.donnees.medias.repository;

import com.immobilier.gestionImmobiliere.donnees.medias.model.Media;
import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, Integer> {
    List<Media> findByEntiteTypeAndEntiteIdOrderByOrdreAsc(TypeEntiteMedia entiteType, Integer entiteId);
    Optional<Media> findByEntiteTypeAndEntiteIdAndIsPrincipalTrue(TypeEntiteMedia entiteType, Integer entiteId);
    long countByEntiteTypeAndEntiteId(TypeEntiteMedia entiteType, Integer entiteId);
}