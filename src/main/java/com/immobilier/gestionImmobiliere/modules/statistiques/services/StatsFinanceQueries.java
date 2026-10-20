package com.immobilier.gestionImmobiliere.modules.statistiques.services;

import com.immobilier.gestionImmobiliere.modules.statistiques.dto.EvolutionMensuelleDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.FinancesPeriodeDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.GainAgenceDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.LoyersStatsDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.ReversementsStatsDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Requêtes agrégées du tableau de bord admin. Les loyers (flux entrant, échéances LOCATION) et les
 * reversements (flux sortant, échéances MANDAT) sont toujours calculés séparément : les additionner
 * n'a aucun sens métier. "Échu" = date d'échéance atteinte ; les échéances ANNULE sont ignorées.
 */
@Component
public class StatsFinanceQueries {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public LoyersStatsDTO loyers() {
        Object[] r = (Object[]) em.createNativeQuery("""
                SELECT
                  COALESCE(SUM(CASE WHEN e.date_echeance <= CURRENT_DATE THEN e.montant_du + e.penalite END), 0),
                  COALESCE(SUM(CASE WHEN e.date_echeance <= CURRENT_DATE THEN COALESCE(e.montant_paye, 0) END), 0),
                  COALESCE(SUM(CASE WHEN e.date_echeance <= CURRENT_DATE AND e.statut <> 'PAYE'
                                    THEN GREATEST(e.montant_du + e.penalite - COALESCE(e.montant_paye, 0), 0) END), 0),
                  COUNT(*) FILTER (WHERE e.date_echeance <= CURRENT_DATE AND e.statut <> 'PAYE'
                                   AND e.montant_du + e.penalite - COALESCE(e.montant_paye, 0) > 0),
                  COALESCE(SUM(CASE WHEN e.date_echeance > CURRENT_DATE THEN e.montant_du END), 0),
                  COALESCE(SUM(CASE WHEN e.date_echeance <= CURRENT_DATE AND e.statut <> 'PAYE' AND CURRENT_DATE - e.date_echeance <= 30
                                    THEN GREATEST(e.montant_du + e.penalite - COALESCE(e.montant_paye, 0), 0) END), 0),
                  COALESCE(SUM(CASE WHEN e.date_echeance <= CURRENT_DATE AND e.statut <> 'PAYE' AND CURRENT_DATE - e.date_echeance BETWEEN 31 AND 60
                                    THEN GREATEST(e.montant_du + e.penalite - COALESCE(e.montant_paye, 0), 0) END), 0),
                  COALESCE(SUM(CASE WHEN e.date_echeance <= CURRENT_DATE AND e.statut <> 'PAYE' AND CURRENT_DATE - e.date_echeance BETWEEN 61 AND 90
                                    THEN GREATEST(e.montant_du + e.penalite - COALESCE(e.montant_paye, 0), 0) END), 0),
                  COALESCE(SUM(CASE WHEN e.date_echeance <= CURRENT_DATE AND e.statut <> 'PAYE' AND CURRENT_DATE - e.date_echeance > 90
                                    THEN GREATEST(e.montant_du + e.penalite - COALESCE(e.montant_paye, 0), 0) END), 0)
                FROM echeance_loyer e
                WHERE e.entite_echeance_type = 'LOCATION' AND e.is_deleted = false AND e.statut <> 'ANNULE'
                """).getSingleResult();

        BigDecimal echu = dec(r[0]);
        BigDecimal encaisse = dec(r[1]);
        BigDecimal taux = echu.signum() == 0 ? BigDecimal.ZERO
                : encaisse.multiply(BigDecimal.valueOf(100)).divide(echu, 1, RoundingMode.HALF_UP).min(BigDecimal.valueOf(100));

        return LoyersStatsDTO.builder()
                .echuTotal(echu)
                .encaisseSurEchu(encaisse)
                .resteAEncaisser(dec(r[2]))
                .nombreImpayesEchus(((Number) r[3]).longValue())
                .tauxRecouvrement(taux)
                .aVenir(dec(r[4]))
                .anciennete(LoyersStatsDTO.AncienneteImpayesDTO.builder()
                        .jours0a30(dec(r[5]))
                        .jours31a60(dec(r[6]))
                        .jours61a90(dec(r[7]))
                        .plusDe90(dec(r[8]))
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public ReversementsStatsDTO reversements() {
        Object[] r = (Object[]) em.createNativeQuery("""
                SELECT
                  COALESCE(SUM(CASE WHEN e.statut = 'PAYE' THEN e.montant_du END), 0),
                  COALESCE(SUM(CASE WHEN e.statut <> 'PAYE' AND e.date_echeance <= CURRENT_DATE
                                    THEN e.montant_du - COALESCE(e.montant_paye, 0) END), 0),
                  COUNT(*) FILTER (WHERE e.statut <> 'PAYE' AND e.date_echeance <= CURRENT_DATE),
                  COALESCE(SUM(CASE WHEN e.statut <> 'PAYE' AND e.date_echeance > CURRENT_DATE THEN e.montant_du END), 0),
                  COALESCE(SUM(CASE WHEN e.statut = 'PAYE' THEN e.commission_deduite END), 0),
                  COALESCE(SUM(CASE WHEN e.statut <> 'PAYE' THEN e.commission_deduite END), 0)
                FROM echeance_loyer e
                WHERE e.entite_echeance_type = 'MANDAT' AND e.is_deleted = false AND e.statut <> 'ANNULE'
                """).getSingleResult();

        return ReversementsStatsDTO.builder()
                .effectue(dec(r[0]))
                .aReverser(dec(r[1]))
                .nombreAReverser(((Number) r[2]).longValue())
                .aVenir(dec(r[3]))
                .commissionsAcquises(dec(r[4]))
                .commissionsPrevues(dec(r[5]))
                .build();
    }

    /** Gain d'un mois (reversements dont l'échéance tombe dans ce mois) : acquis (payé) et prévu (pas encore payé). */
    @Transactional(readOnly = true)
    public GainAgenceDTO gainDuMois(LocalDate debutMois, String libellePeriode) {
        Object[] r = (Object[]) em.createNativeQuery("""
                SELECT
                  COALESCE(SUM(CASE WHEN e.statut = 'PAYE' THEN e.commission_deduite END), 0),
                  COALESCE(SUM(CASE WHEN e.statut <> 'PAYE' THEN e.commission_deduite END), 0),
                  COALESCE(SUM(CASE WHEN e.statut = 'PAYE' THEN e.montant_du END), 0),
                  COALESCE(SUM(CASE WHEN e.statut <> 'PAYE' THEN e.montant_du END), 0)
                FROM echeance_loyer e
                WHERE e.entite_echeance_type = 'MANDAT' AND e.is_deleted = false AND e.statut <> 'ANNULE'
                  AND DATE_TRUNC('month', e.date_echeance) = DATE_TRUNC('month', CAST(:periode AS date))
                """).setParameter("periode", Date.valueOf(debutMois)).getSingleResult();

        return GainAgenceDTO.builder()
                .periodeMois(libellePeriode)
                .commissionsAcquises(dec(r[0]))
                .commissionsPrevues(dec(r[1]))
                .reverseEffectue(dec(r[2]))
                .reversePrevu(dec(r[3]))
                .build();
    }

    /** Série mensuelle sur les {@code nbMois} derniers mois (mois courant inclus), mois vides compris. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<EvolutionMensuelleDTO> evolution(int nbMois) {
        List<Object[]> rows = em.createNativeQuery("""
                WITH m AS (
                  SELECT CAST(generate_series(DATE_TRUNC('month', CURRENT_DATE) - ((CAST(:n AS int) - 1) * INTERVAL '1 month'),
                                              DATE_TRUNC('month', CURRENT_DATE), INTERVAL '1 month') AS date) AS mois)
                SELECT m.mois,
                  COALESCE((SELECT SUM(e.montant_du + e.penalite) FROM echeance_loyer e
                            WHERE e.entite_echeance_type = 'LOCATION' AND e.is_deleted = false AND e.statut <> 'ANNULE'
                              AND CAST(DATE_TRUNC('month', e.date_echeance) AS date) = m.mois), 0),
                  COALESCE((SELECT SUM(p.montant_paiement) FROM paiement p
                            JOIN paiement_echeance pe ON pe.id_paiement = p.id_paiement AND pe.is_deleted = false
                            JOIN echeance_loyer e ON e.id_echeance = pe.id_echeance
                            WHERE e.entite_echeance_type = 'LOCATION' AND p.sens = 'ENTREE' AND p.is_deleted = false
                              AND CAST(DATE_TRUNC('month', p.date_paiement) AS date) = m.mois), 0),
                  COALESCE((SELECT SUM(p.montant_paiement) FROM paiement p
                            JOIN paiement_echeance pe ON pe.id_paiement = p.id_paiement AND pe.is_deleted = false
                            JOIN echeance_loyer e ON e.id_echeance = pe.id_echeance
                            WHERE e.entite_echeance_type = 'MANDAT' AND p.is_deleted = false
                              AND CAST(DATE_TRUNC('month', p.date_paiement) AS date) = m.mois), 0),
                  COALESCE((SELECT SUM(e.commission_deduite) FROM echeance_loyer e
                            WHERE e.entite_echeance_type = 'MANDAT' AND e.is_deleted = false AND e.statut = 'PAYE'
                              AND CAST(DATE_TRUNC('month', e.date_echeance) AS date) = m.mois), 0),
                  (SELECT COUNT(*) FROM users u WHERE u.is_deleted = false
                     AND CAST(DATE_TRUNC('month', u.created_at) AS date) = m.mois),
                  (SELECT COUNT(*) FROM contra_location c WHERE c.is_deleted = false
                     AND CAST(DATE_TRUNC('month', c.created_at) AS date) = m.mois),
                  COALESCE((SELECT SUM(p.montant_paiement) FROM paiement p
                            JOIN paiement_location_bien_service pl ON pl.id_paiement = p.id_paiement AND pl.is_deleted = false
                            WHERE p.sens = 'ENTREE' AND p.is_deleted = false
                              AND CAST(DATE_TRUNC('month', p.date_paiement) AS date) = m.mois), 0)
                  - COALESCE((SELECT SUM(p.montant_paiement) FROM remboursement r
                              JOIN paiement p ON p.id_paiement = r.id_paiement
                              WHERE r.entite_type = 'LOCATION_BIEN_SERVICE' AND r.is_deleted = false AND p.is_deleted = false
                                AND CAST(DATE_TRUNC('month', p.date_paiement) AS date) = m.mois), 0)
                FROM m ORDER BY m.mois
                """).setParameter("n", nbMois).getResultList();

        return rows.stream().map(r -> EvolutionMensuelleDTO.builder()
                .mois(r[0] instanceof Date d ? d.toLocalDate() : (LocalDate) r[0])
                .loyersAttendus(dec(r[1]))
                .loyersEncaisses(dec(r[2]))
                .reversementsEffectues(dec(r[3]))
                .commissionsAcquises(dec(r[4]))
                .inscriptions(((Number) r[5]).longValue())
                .nouveauxContrats(((Number) r[6]).longValue())
                .biensServicesNet(dec(r[7]))
                .build()).toList();
    }

    private static final LocalDate DEBUT_DES_TEMPS = LocalDate.of(1900, 1, 1);
    private static final LocalDate FIN_DES_TEMPS = LocalDate.of(2999, 12, 31);

    /**
     * Flux d'une période : paiements datés dans [debut, fin] (bornes incluses, null = sans borne).
     * Les commissions suivent la date d'échéance du reversement payé, comme dans la série d'évolution.
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public FinancesPeriodeDTO finances(LocalDate debut, LocalDate fin) {
        Date d = Date.valueOf(debut != null ? debut : DEBUT_DES_TEMPS);
        Date f = Date.valueOf(fin != null ? fin : FIN_DES_TEMPS);

        BigDecimal loyers = dec(em.createNativeQuery("""
                SELECT COALESCE(SUM(p.montant_paiement), 0) FROM paiement p
                JOIN paiement_echeance pe ON pe.id_paiement = p.id_paiement AND pe.is_deleted = false
                JOIN echeance_loyer e ON e.id_echeance = pe.id_echeance
                WHERE e.entite_echeance_type = 'LOCATION' AND p.sens = 'ENTREE' AND p.is_deleted = false
                  AND p.date_paiement >= :d AND p.date_paiement < CAST(:f AS date) + 1
                """).setParameter("d", d).setParameter("f", f).getSingleResult());

        BigDecimal reversements = dec(em.createNativeQuery("""
                SELECT COALESCE(SUM(p.montant_paiement), 0) FROM paiement p
                JOIN paiement_echeance pe ON pe.id_paiement = p.id_paiement AND pe.is_deleted = false
                JOIN echeance_loyer e ON e.id_echeance = pe.id_echeance
                WHERE e.entite_echeance_type = 'MANDAT' AND p.is_deleted = false
                  AND p.date_paiement >= :d AND p.date_paiement < CAST(:f AS date) + 1
                """).setParameter("d", d).setParameter("f", f).getSingleResult());

        BigDecimal commissions = dec(em.createNativeQuery("""
                SELECT COALESCE(SUM(e.commission_deduite), 0) FROM echeance_loyer e
                WHERE e.entite_echeance_type = 'MANDAT' AND e.is_deleted = false AND e.statut = 'PAYE'
                  AND e.date_echeance >= :d AND e.date_echeance <= :f
                """).setParameter("d", d).setParameter("f", f).getSingleResult());

        List<Object[]> encaisses = em.createNativeQuery("""
                SELECT c.libelle, COALESCE(SUM(p.montant_paiement), 0), COUNT(DISTINCT l.id_location_bien_service)
                FROM paiement p
                JOIN paiement_location_bien_service pl ON pl.id_paiement = p.id_paiement AND pl.is_deleted = false
                JOIN location_bien_service l ON l.id_location_bien_service = pl.id_location_bien_service
                JOIN bien_service b ON b.id_bien_service = l.id_bien_service
                JOIN categorie_bien_service c ON c.id_categorie = b.id_categorie
                WHERE p.sens = 'ENTREE' AND p.is_deleted = false
                  AND p.date_paiement >= :d AND p.date_paiement < CAST(:f AS date) + 1
                GROUP BY c.libelle
                """).setParameter("d", d).setParameter("f", f).getResultList();

        List<Object[]> rembourses = em.createNativeQuery("""
                SELECT c.libelle, COALESCE(SUM(p.montant_paiement), 0)
                FROM remboursement r
                JOIN paiement p ON p.id_paiement = r.id_paiement
                JOIN location_bien_service l ON l.id_location_bien_service = r.entite_id
                JOIN bien_service b ON b.id_bien_service = l.id_bien_service
                JOIN categorie_bien_service c ON c.id_categorie = b.id_categorie
                WHERE r.entite_type = 'LOCATION_BIEN_SERVICE' AND r.is_deleted = false AND p.is_deleted = false
                  AND p.date_paiement >= :d AND p.date_paiement < CAST(:f AS date) + 1
                GROUP BY c.libelle
                """).setParameter("d", d).setParameter("f", f).getResultList();

        Map<String, FinancesPeriodeDTO.CategorieFinanceDTO> parCategorie = new LinkedHashMap<>();
        for (Object[] r : encaisses) {
            BigDecimal enc = dec(r[1]);
            parCategorie.put(String.valueOf(r[0]), FinancesPeriodeDTO.CategorieFinanceDTO.builder()
                    .categorie(String.valueOf(r[0])).encaisse(enc).rembourse(BigDecimal.ZERO).net(enc)
                    .nbLocations(((Number) r[2]).longValue()).build());
        }
        for (Object[] r : rembourses) {
            BigDecimal remb = dec(r[1]);
            FinancesPeriodeDTO.CategorieFinanceDTO existante = parCategorie.get(String.valueOf(r[0]));
            if (existante == null) {
                parCategorie.put(String.valueOf(r[0]), FinancesPeriodeDTO.CategorieFinanceDTO.builder()
                        .categorie(String.valueOf(r[0])).encaisse(BigDecimal.ZERO).rembourse(remb).net(remb.negate())
                        .nbLocations(0).build());
            } else {
                existante.setRembourse(remb);
                existante.setNet(existante.getEncaisse().subtract(remb));
            }
        }
        List<FinancesPeriodeDTO.CategorieFinanceDTO> categories = new ArrayList<>(parCategorie.values());
        categories.sort((a, b) -> b.getNet().compareTo(a.getNet()));

        BigDecimal bsEncaisse = categories.stream().map(FinancesPeriodeDTO.CategorieFinanceDTO::getEncaisse).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bsRembourse = categories.stream().map(FinancesPeriodeDTO.CategorieFinanceDTO::getRembourse).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bsNet = bsEncaisse.subtract(bsRembourse);
        long bsNb = categories.stream().mapToLong(FinancesPeriodeDTO.CategorieFinanceDTO::getNbLocations).sum();

        return FinancesPeriodeDTO.builder()
                .debut(debut).fin(fin)
                .loyersEncaisses(loyers)
                .reversementsEffectues(reversements)
                .commissionsAcquises(commissions)
                .biensServices(FinancesPeriodeDTO.BiensServicesFinancesDTO.builder()
                        .encaisse(bsEncaisse).rembourse(bsRembourse).net(bsNet).nbLocations(bsNb).parCategorie(categories).build())
                .volumeEncaisse(loyers.add(bsNet))
                .revenuAgence(commissions.add(bsNet))
                .build();
    }

    /** Montants par ville (clé = nom de la ville) : une requête par indicateur, fusionnées par l'appelant. */
    public enum IndicateurVille { LOYERS, BS_ENCAISSE, BS_REMBOURSE, COMMISSIONS, IMPAYES }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<IndicateurVille, Map<String, BigDecimal>> valeurParVille(LocalDate debut, LocalDate fin) {
        Date d = Date.valueOf(debut != null ? debut : DEBUT_DES_TEMPS);
        Date f = Date.valueOf(fin != null ? fin : FIN_DES_TEMPS);
        String rattacheMaison = """
                JOIN contra_location cl ON cl.id_contra_location = e.entite_echeance_id
                JOIN maison m ON m.id_maison = cl.id_maison
                JOIN cour c ON c.id_cour = m.id_cour
                JOIN secteur s ON s.id_secteur = c.id_secteur
                JOIN ville v ON v.id_ville = s.id_ville
                """;
        Map<IndicateurVille, Map<String, BigDecimal>> res = new java.util.EnumMap<>(IndicateurVille.class);

        res.put(IndicateurVille.LOYERS, parVille(em.createNativeQuery("""
                SELECT v.nom_ville, SUM(p.montant_paiement) FROM paiement p
                JOIN paiement_echeance pe ON pe.id_paiement = p.id_paiement AND pe.is_deleted = false
                JOIN echeance_loyer e ON e.id_echeance = pe.id_echeance AND e.entite_echeance_type = 'LOCATION'
                """ + rattacheMaison + """
                WHERE p.sens = 'ENTREE' AND p.is_deleted = false
                  AND p.date_paiement >= :d AND p.date_paiement < CAST(:f AS date) + 1
                GROUP BY v.nom_ville
                """).setParameter("d", d).setParameter("f", f).getResultList()));

        res.put(IndicateurVille.IMPAYES, parVille(em.createNativeQuery("""
                SELECT v.nom_ville, SUM(GREATEST(e.montant_du + e.penalite - COALESCE(e.montant_paye, 0), 0))
                FROM echeance_loyer e
                """ + rattacheMaison + """
                WHERE e.entite_echeance_type = 'LOCATION' AND e.is_deleted = false
                  AND e.statut NOT IN ('PAYE', 'ANNULE') AND e.date_echeance <= CURRENT_DATE
                GROUP BY v.nom_ville
                """).getResultList()));

        res.put(IndicateurVille.COMMISSIONS, parVille(em.createNativeQuery("""
                SELECT v.nom_ville, SUM(e.commission_deduite) FROM echeance_loyer e
                JOIN contrat_mandat cm ON cm.id_mandat = e.entite_echeance_id
                JOIN cour c ON c.id_cour = cm.id_cour
                JOIN secteur s ON s.id_secteur = c.id_secteur
                JOIN ville v ON v.id_ville = s.id_ville
                WHERE e.entite_echeance_type = 'MANDAT' AND e.is_deleted = false AND e.statut = 'PAYE'
                  AND e.date_echeance >= :d AND e.date_echeance <= :f
                GROUP BY v.nom_ville
                """).setParameter("d", d).setParameter("f", f).getResultList()));

        res.put(IndicateurVille.BS_ENCAISSE, parVille(em.createNativeQuery("""
                SELECT v.nom_ville, SUM(p.montant_paiement) FROM paiement p
                JOIN paiement_location_bien_service pl ON pl.id_paiement = p.id_paiement AND pl.is_deleted = false
                JOIN location_bien_service l ON l.id_location_bien_service = pl.id_location_bien_service
                JOIN bien_service b ON b.id_bien_service = l.id_bien_service
                JOIN secteur s ON s.id_secteur = b.id_secteur
                JOIN ville v ON v.id_ville = s.id_ville
                WHERE p.sens = 'ENTREE' AND p.is_deleted = false
                  AND p.date_paiement >= :d AND p.date_paiement < CAST(:f AS date) + 1
                GROUP BY v.nom_ville
                """).setParameter("d", d).setParameter("f", f).getResultList()));

        res.put(IndicateurVille.BS_REMBOURSE, parVille(em.createNativeQuery("""
                SELECT v.nom_ville, SUM(p.montant_paiement) FROM remboursement r
                JOIN paiement p ON p.id_paiement = r.id_paiement
                JOIN location_bien_service l ON l.id_location_bien_service = r.entite_id
                JOIN bien_service b ON b.id_bien_service = l.id_bien_service
                JOIN secteur s ON s.id_secteur = b.id_secteur
                JOIN ville v ON v.id_ville = s.id_ville
                WHERE r.entite_type = 'LOCATION_BIEN_SERVICE' AND r.is_deleted = false AND p.is_deleted = false
                  AND p.date_paiement >= :d AND p.date_paiement < CAST(:f AS date) + 1
                GROUP BY v.nom_ville
                """).setParameter("d", d).setParameter("f", f).getResultList()));
        return res;
    }

    private static Map<String, BigDecimal> parVille(List<Object[]> rows) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        for (Object[] r : rows) m.put(String.valueOf(r[0]), dec(r[1]));
        return m;
    }

    private static BigDecimal dec(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }
}
