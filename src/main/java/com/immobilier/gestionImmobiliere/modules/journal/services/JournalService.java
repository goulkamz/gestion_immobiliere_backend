package com.immobilier.gestionImmobiliere.modules.journal.services;

import com.immobilier.gestionImmobiliere.donnees.journal.model.JournalOperation;
import com.immobilier.gestionImmobiliere.donnees.journal.repository.JournalOperationRepository;
import com.immobilier.gestionImmobiliere.modules.journal.dto.responses.JournalResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class JournalService {

    private final JournalOperationRepository journalRepository;

    public JournalService(JournalOperationRepository journalRepository) {
        this.journalRepository = journalRepository;
    }

    public ResponseEntity<?> getAll(Integer idUser, String action, String entite,
                                    LocalDateTime dateDebut, LocalDateTime dateFin, Pageable pageable) {
        Page<JournalResponseDTO> page = journalRepository
                .search(idUser, action, entite, dateDebut, dateFin, pageable)
                .map(this::toDto);
        return buildSuccessResponse(HttpStatus.OK, "Journal des opérations", "JOURNAL_LIST", page);
    }

    private JournalResponseDTO toDto(JournalOperation j) {
        return JournalResponseDTO.builder()
                .idJournal(j.getIdJournal())
                .idUser(j.getIdUser())
                .action(j.getAction())
                .entite(j.getEntite())
                .ligneEntite(j.getLigneEntite())
                .description(j.getDescription())
                .dateAction(j.getDateAction())
                .ancienContenu(j.getAncienContenu())
                .nouveauContenu(j.getNouveauContenu())
                .build();
    }
}