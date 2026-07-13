package com.immobilier.gestionImmobiliere.donnees.medias.model;

import com.immobilier.gestionImmobiliere.donnees.Model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "medias")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE medias SET is_deleted = true WHERE id_media = ?")
@Where(clause = "is_deleted = false")
public class Media extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_media")
    private Integer idMedia;

    @Enumerated(EnumType.STRING)
    @Column(name = "entite_type", nullable = false)
    private TypeEntiteMedia entiteType;

    @Column(name = "entite_id", nullable = false)
    private Integer entiteId;

    @Column(name = "type_media")
    private String typeMedia; // ex: "image/jpeg" (content-type stocké)

    @Column(name = "media_path", nullable = false)
    private String mediaPath;

    @Column(name = "media_path_thumbnail")
    private String mediaPathThumbnail;

    @Column(name = "is_principal")
    @Builder.Default
    private Boolean isPrincipal = false;

    @Column(name = "ordre")
    @Builder.Default
    private Short ordre = 0;

    @Column(name = "date_upload")
    private LocalDateTime dateUpload;
}