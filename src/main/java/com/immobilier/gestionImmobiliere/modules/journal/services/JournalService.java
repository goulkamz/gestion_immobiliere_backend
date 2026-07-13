package com.immobilier.gestionImmobiliere.modules.journal.services;

import com.immobilier.gestionImmobiliere.donnees.journal.model.JournalOperation;
import com.immobilier.gestionImmobiliere.donnees.journal.repository.JournalOperationRepository;
import com.immobilier.gestionImmobiliere.modules.journal.dto.responses.JournalResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Enregistre une entrée d'audit. PROPAGATION.REQUIRES_NEW : le log doit être écrit
     * même si la transaction métier appelante échoue ensuite et fait un rollback —
     * sinon on perdrait la trace de l'action qui a provoqué l'erreur.
     * À l'inverse, un échec d'écriture du log ne doit JAMAIS faire échouer l'opération
     * métier elle-même (d'où le try/catch qui avale l'exception, en dernier recours).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(Integer idUser, String action, String entite, Integer ligneEntite,
                            String description, String ancienContenu, String nouveauContenu) {
        try {
            JournalOperation log = JournalOperation.builder()
                    .idUser(idUser)
                    .action(action)
                    .entite(entite)
                    .ligneEntite(ligneEntite)
                    .description(description)
                    .dateAction(LocalDateTime.now())
                    .ancienContenu(tronquer(ancienContenu))
                    .nouveauContenu(tronquer(nouveauContenu))
                    .build();
            journalRepository.save(log);
        } catch (Exception e) {
            // Le journal ne doit jamais bloquer une opération métier — on log l'échec côté SLF4J
            // (via un logger dans une vraie implémentation) et on continue.
        }
    }

    private String tronquer(String contenu) {
        if (contenu == null) return null;
        // Colonne CHAR(254) dans le schéma -> troncature défensive côté application
        return contenu.length() > 254 ? contenu.substring(0, 254) : contenu;
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