package com.immobilier.gestionImmobiliere.donnees.notifications.repository;

import com.immobilier.gestionImmobiliere.donnees.notifications.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

// Toutes les lectures/ecritures sont filtrees par idUser : l'isolation ne repose jamais sur l'id de la notification seul
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Page<Notification> findByIdUser(Integer idUser, Pageable pageable);

    Page<Notification> findByIdUserAndLue(Integer idUser, Boolean lue, Pageable pageable);

    Optional<Notification> findByIdNotificationAndIdUser(Integer idNotification, Integer idUser);

    long countByIdUserAndLueFalse(Integer idUser);

    boolean existsByIdUserAndCleAlerte(Integer idUser, String cleAlerte);

    @Modifying
    @Query("UPDATE Notification n SET n.lue = true, n.dateLecture = :now, n.updatedAt = :now WHERE n.idUser = :idUser AND n.lue = false")
    int marquerToutesLues(@Param("idUser") Integer idUser, @Param("now") LocalDateTime now);

    // Suppression physique (JPQL bulk : contourne @SQLDelete) des notifications LUES anciennes ; les non lues sont conservees
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.lue = true AND n.dateLecture < :limite")
    int purgerLuesAvant(@Param("limite") LocalDateTime limite);
}
