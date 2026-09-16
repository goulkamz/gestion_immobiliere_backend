package com.immobilier.gestionImmobiliere.donnees.documents.repository;
import com.immobilier.gestionImmobiliere.donnees.documents.model.Document;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeEntiteDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Integer> {
    List<Document> findByEntiteTypeAndEntiteIdOrderByCreatedAtDesc(TypeEntiteDocument type, Integer entiteId);

    Optional<Document> findByTypeDocumentAndPeriodeMois(TypeDocument type, LocalDate periodeMois);
}