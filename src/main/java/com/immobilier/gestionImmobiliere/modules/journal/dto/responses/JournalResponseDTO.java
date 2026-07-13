package com.immobilier.gestionImmobiliere.modules.journal.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class JournalResponseDTO {
    private Integer idJournal;
    private Integer idUser;
    private String action;
    private String entite;
    private Integer ligneEntite;
    private String description;
    private LocalDateTime dateAction;
    private String ancienContenu;
    private String nouveauContenu;
}