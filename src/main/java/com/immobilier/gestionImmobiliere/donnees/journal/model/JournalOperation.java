package com.immobilier.gestionImmobiliere.donnees.journal.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import com.immobilier.gestionImmobiliere.donnees.Model_1;
import com.immobilier.gestionImmobiliere.utils.CustomDate;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "journal_operation")
@Data @Builder  @AllArgsConstructor
@SQLDelete(sql = "UPDATE journal_operation SET is_deleted = true WHERE id_journal = ?")
@Where(clause = "is_deleted = false")
public class JournalOperation extends Model_1 {

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

    @Column(name = "ancien_contenu",columnDefinition = "jsonb")
    private String ancienContenu;

    @Column(name = "nouveau_contenu",columnDefinition = "jsonb")
    private String nouveauContenu;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public JournalOperation(){initTimestamp();}

    public void initTimestamp() {
        this.createdAt = CustomDate.now();
        this.updatedAt = CustomDate.now();
    }
}