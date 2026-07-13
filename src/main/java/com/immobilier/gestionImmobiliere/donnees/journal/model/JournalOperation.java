package com.immobilier.gestionImmobiliere.donnees.journal.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "journal_operation")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE journal_operation SET is_deleted = true WHERE id_journal = ?")
@Where(clause = "is_deleted = false")
public class JournalOperation extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_journal")
    private Integer idJournal;

    @Column(name = "id_user", nullable = false)
    private Integer idUser;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entite", nullable = false)
    private String entite;

    @Column(name = "ligne_entite")
    private Integer ligneEntite;

    @Column(name = "description")
    private String description;

    @Column(name = "date_action")
    private LocalDateTime dateAction;

    @Column(name = "ancien_contenu")
    private String ancienContenu;

    @Column(name = "nouveau_contenu")
    private String nouveauContenu;
}