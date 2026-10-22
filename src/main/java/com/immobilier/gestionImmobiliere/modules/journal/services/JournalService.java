package com.immobilier.gestionImmobiliere.modules.journal.services;

import com.immobilier.gestionImmobiliere.donnees.journal.model.JournalOperation;
import com.immobilier.gestionImmobiliere.donnees.journal.repository.JournalOperationRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.modules.journal.dto.responses.JournalResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class JournalService {

    private final JournalOperationRepository journalRepository;
    private final UserRepository userRepository;

    public JournalService(JournalOperationRepository journalRepository, UserRepository userRepository) {
        this.journalRepository = journalRepository;
        this.userRepository = userRepository;
    }

    public ResponseEntity<?> getAll(Integer idUser, String action, String entite,
                                    LocalDate dateDebut, LocalDate dateFin, Pageable pageable) {
        LocalDateTime debut = dateDebut != null ? dateDebut.atStartOfDay() : null;
        LocalDateTime fin = dateFin != null ? dateFin.atTime(LocalTime.MAX) : null;
        Page<JournalOperation> page = journalRepository.search(idUser, action, entite, debut, fin, pageable);

        // Un seul accès pour résoudre les noms des auteurs de la page
        Set<Integer> ids = page.getContent().stream().map(JournalOperation::getIdUser).collect(Collectors.toSet());
        Map<Integer, User> auteurs = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getIdUser, Function.identity()));

        return buildSuccessResponse(HttpStatus.OK, "Journal des opérations", "JOURNAL_LIST",
                page.map(j -> toDto(j, auteurs.get(j.getIdUser()))));
    }

    public ResponseEntity<?> getEntites() {
        List<String> entites = journalRepository.findEntitesDistinctes();
        return buildSuccessResponse(HttpStatus.OK, "Types d'éléments journalisés", "JOURNAL_ENTITES", entites);
    }

    private JournalResponseDTO toDto(JournalOperation j, User auteur) {
        return JournalResponseDTO.builder()
                .idJournal(j.getIdJournal())
                .idUser(j.getIdUser())
                .auteur(auteur != null ? (auteur.getPrenom() + " " + auteur.getNom()).trim() : null)
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
