package com.immobilier.gestionImmobiliere.donnees.annonces.repository;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Annonce;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutAnnonce;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AnnonceRepository extends JpaRepository<Annonce, Integer> {
    Page<Annonce> findByStatut(StatutAnnonce statut, Pageable pageable);
    List<Annonce> findByStatutAndDateExpirationBefore(StatutAnnonce statut, LocalDate date);
}