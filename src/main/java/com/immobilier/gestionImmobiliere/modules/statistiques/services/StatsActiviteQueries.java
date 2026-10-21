package com.immobilier.gestionImmobiliere.modules.statistiques.services;

import com.immobilier.gestionImmobiliere.modules.statistiques.dto.ActivitePeriodeDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;

/** Combien d'éléments ont été créés ou reçus pendant une période (comptés par date de création, hors supprimés). */
@Component
public class StatsActiviteQueries {

    private static final LocalDate DEBUT_DES_TEMPS = LocalDate.of(1900, 1, 1);
    private static final LocalDate FIN_DES_TEMPS = LocalDate.of(2999, 12, 31);

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public ActivitePeriodeDTO activite(LocalDate debut, LocalDate fin) {
        Date d = Date.valueOf(debut != null ? debut : DEBUT_DES_TEMPS);
        Date f = Date.valueOf(fin != null ? fin : FIN_DES_TEMPS);

        Object[] r = (Object[]) em.createNativeQuery("""
                SELECT
                  (SELECT COUNT(*) FROM contrat_mandat t WHERE t.is_deleted = false
                     AND t.created_at >= :d AND t.created_at < CAST(:f AS date) + 1),
                  (SELECT COUNT(*) FROM contra_location t WHERE t.is_deleted = false
                     AND t.created_at >= :d AND t.created_at < CAST(:f AS date) + 1),
                  (SELECT COUNT(*) FROM location_bien_service t WHERE t.is_deleted = false
                     AND t.created_at >= :d AND t.created_at < CAST(:f AS date) + 1),
                  (SELECT COUNT(*) FROM annonce t WHERE t.is_deleted = false
                     AND t.created_at >= :d AND t.created_at < CAST(:f AS date) + 1),
                  (SELECT COUNT(*) FROM demande t WHERE t.is_deleted = false
                     AND t.created_at >= :d AND t.created_at < CAST(:f AS date) + 1)
                """).setParameter("d", d).setParameter("f", f).getSingleResult();

        return ActivitePeriodeDTO.builder()
                .debut(debut).fin(fin)
                .mandats(((Number) r[0]).longValue())
                .contratsLocation(((Number) r[1]).longValue())
                .locationsBienService(((Number) r[2]).longValue())
                .annonces(((Number) r[3]).longValue())
                .demandes(((Number) r[4]).longValue())
                .build();
    }
}
