package com.immobilier.gestionImmobiliere.modules.statistiques.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutContact;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutOffre;
import com.immobilier.gestionImmobiliere.donnees.annonces.repository.*;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.*;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.*;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.*;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.*;
import com.immobilier.gestionImmobiliere.utils.DateUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class AdminStatsService {

    private final UserRepository userRepository;
    private final CourRepository courRepository;
    private final MaisonRepository maisonRepository;
    private final BienServiceRepository bienServiceRepository;
    private final ContratMandatRepository contratMandatRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final EcheanceLoyerRepository echeanceLoyerRepository;
    private final LocationBienServiceRepository locationBienServiceRepository;
    private final PaiementLocationBienServiceRepository paiementLocationRepository;
    private final RemboursementRepository remboursementRepository;
    private final AnnonceRepository annonceRepository;
    private final DemandeRepository demandeRepository;
    private final OffreRepository offreRepository;
    private final ContactRepository contactRepository;
    private final StatsFinanceQueries financeQueries;
    private final StatsCache cache;
    private final StatsActiviteQueries activiteQueries;

    public AdminStatsService(UserRepository userRepository, CourRepository courRepository, MaisonRepository maisonRepository,
                             BienServiceRepository bienServiceRepository, ContratMandatRepository contratMandatRepository,
                             ContratLocationRepository contratLocationRepository, EcheanceLoyerRepository echeanceLoyerRepository,
                             LocationBienServiceRepository locationBienServiceRepository,
                             PaiementLocationBienServiceRepository paiementLocationRepository,
                             RemboursementRepository remboursementRepository, AnnonceRepository annonceRepository,
                             DemandeRepository demandeRepository, OffreRepository offreRepository,
                             ContactRepository contactRepository, StatsFinanceQueries financeQueries,
                             StatsCache cache, StatsActiviteQueries activiteQueries) {
        this.userRepository = userRepository;
        this.courRepository = courRepository;
        this.maisonRepository = maisonRepository;
        this.bienServiceRepository = bienServiceRepository;
        this.contratMandatRepository = contratMandatRepository;
        this.contratLocationRepository = contratLocationRepository;
        this.echeanceLoyerRepository = echeanceLoyerRepository;
        this.locationBienServiceRepository = locationBienServiceRepository;
        this.paiementLocationRepository = paiementLocationRepository;
        this.remboursementRepository = remboursementRepository;
        this.annonceRepository = annonceRepository;
        this.demandeRepository = demandeRepository;
        this.offreRepository = offreRepository;
        this.contactRepository = contactRepository;
        this.financeQueries = financeQueries;
        this.cache = cache;
        this.activiteQueries = activiteQueries;
    }

    public ResponseEntity<?> getStats() {
        return buildSuccessResponse(HttpStatus.OK, "Statistiques administrateur", "ADMIN_STATS", cache.get("stats", this::calculerStats));
    }

    /** Vide le cache des statistiques (bouton « Actualiser »). */
    public ResponseEntity<?> viderCache() {
        cache.vider();
        return buildSuccessResponse(HttpStatus.OK, "Cache des statistiques vidé", "STATS_CACHE_VIDE", null);
    }

    private AdminStatsDTO calculerStats() {
        BigDecimal encaisse    = defaultZero(paiementLocationRepository.sumTotalEncaisse().getTotal());
        BigDecimal rembourse   = defaultZero(remboursementRepository.sumRembourseBienService());

        AdminStatsDTO stats = AdminStatsDTO.builder()
                .totalUtilisateurs(userRepository.countByIsDeletedFalse())
                .utilisateursParRole(toMap(userRepository.countByRole()))
                .inscriptionsCeMois(userRepository.countInscriptionsCeMois())

                .totalCours(courRepository.countByIsDeletedFalse())
                .maisonsParStatut(toMap(maisonRepository.countByStatut()))
                .tauxOccupationMaisons(maisonRepository.tauxOccupation().setScale(2, RoundingMode.HALF_UP))
                .biensServiceParDisponibilite(toMapImbrique(bienServiceRepository.countByCategorieEtDisponibilite()))

                .mandatsParStatut(toMap(contratMandatRepository.countByStatut()))
                .contratsLocationParStatut(toMap(contratLocationRepository.countByStatut()))
                .mandatsExpirantSous30Jours(contratMandatRepository.countExpirantSous30Jours())

                .loyers(financeQueries.loyers())
                .reversements(financeQueries.reversements())

                .locationsBienServiceParStatut(toMap(locationBienServiceRepository.countByStatut()))
                .totalEncaisseBienService(encaisse)
                .totalRembourseBienService(rembourse)

                .annoncesParStatut(toMap(annonceRepository.countByStatut()))
                .demandesParStatut(toMap(demandeRepository.countByStatut()))
                .offresNonTraitees(offreRepository.countByIsDeletedFalseAndStatut(StatutOffre.EN_ATTENTE))
                .contactsNonLus(contactRepository.countByIsDeletedFalseAndStatut(StatutContact.NON_LU))

                .connexionsRecentes7j(userRepository.countConnexionsRecentes())
                .build();

        return stats;
    }


    public ResponseEntity<?> getStatsParVille(LocalDate debut, LocalDate fin) {
        return buildSuccessResponse(HttpStatus.OK, "Statistiques par ville", "STATS_PAR_VILLE",
                cache.get("villes|" + debut + "|" + fin, () -> calculerStatsParVille(debut, fin)));
    }

    private List<VilleStatsDTO> calculerStatsParVille(LocalDate debut, LocalDate fin) {
        List<Object[]> maisonsParVille = maisonRepository.countMaisonsParVille();
        List<Object[]> locatairesParVille = contratLocationRepository.countLocatairesActifsParVille();

        Map<String, Long> nbMaisonsMap = new HashMap<>();
        for (Object[] row : maisonsParVille) nbMaisonsMap.put((String) row[0], ((Number) row[1]).longValue());
        Map<String, Long> nbLocatairesMap = new HashMap<>();
        for (Object[] row : locatairesParVille) nbLocatairesMap.put((String) row[0], ((Number) row[1]).longValue());

        var valeurs = financeQueries.valeurParVille(debut, fin);

        // Toutes les villes qui ont de l'activité : maisons, locataires ou revenus (ex. ville avec seulement des biens et services)
        java.util.Set<String> villes = new java.util.LinkedHashSet<>(nbMaisonsMap.keySet());
        villes.addAll(nbLocatairesMap.keySet());
        valeurs.values().forEach(m -> villes.addAll(m.keySet()));

        List<VilleStatsDTO> result = villes.stream()
                .map(ville -> {
                    BigDecimal loyers = valeurs.get(StatsFinanceQueries.IndicateurVille.LOYERS).getOrDefault(ville, BigDecimal.ZERO);
                    BigDecimal bsNet = valeurs.get(StatsFinanceQueries.IndicateurVille.BS_ENCAISSE).getOrDefault(ville, BigDecimal.ZERO)
                            .subtract(valeurs.get(StatsFinanceQueries.IndicateurVille.BS_REMBOURSE).getOrDefault(ville, BigDecimal.ZERO));
                    BigDecimal commissions = valeurs.get(StatsFinanceQueries.IndicateurVille.COMMISSIONS).getOrDefault(ville, BigDecimal.ZERO);
                    return VilleStatsDTO.builder()
                            .nomVille(ville)
                            .nbMaisons(nbMaisonsMap.getOrDefault(ville, 0L))
                            .nbLocatairesActifs(nbLocatairesMap.getOrDefault(ville, 0L))
                            .loyersEncaisses(loyers)
                            .biensServicesNet(bsNet)
                            .commissionsAcquises(commissions)
                            .volumeEncaisse(loyers.add(bsNet))
                            .revenuAgence(commissions.add(bsNet))
                            .impayesEchus(valeurs.get(StatsFinanceQueries.IndicateurVille.IMPAYES).getOrDefault(ville, BigDecimal.ZERO))
                            .build();
                })
                .sorted((a, b) -> b.getVolumeEncaisse().compareTo(a.getVolumeEncaisse()))
                .toList();

        return result;
    }

    public ResponseEntity<?> getTopBailleurs(int limite) {
        return buildSuccessResponse(HttpStatus.OK, "Top bailleurs par nombre de maisons", "TOP_BAILLEURS",
                cache.get("top|" + limite, () -> calculerTopBailleurs(limite)));
    }

    private List<BailleurTopDTO> calculerTopBailleurs(int limite) {
        List<BailleurTopDTO> result = courRepository.topBailleursParNbMaisons(limite).stream()
                .map(row -> BailleurTopDTO.builder()
                        .idBailleur((Integer) row[0])
                        .nomComplet((String) row[1])
                        .nbMaisons(((Number) row[2]).longValue())
                        .loyersEncaisses(toBigDecimal(row[3]))
                        .build())
                .toList();

        return result;
    }

    public ResponseEntity<?> getLocatairesEnCreance() {
        return buildSuccessResponse(HttpStatus.OK, "Locataires en créance", "LOCATAIRES_EN_CREANCE",
                cache.get("creances", this::calculerLocatairesEnCreance));
    }

    private List<LocataireCreanceDTO> calculerLocatairesEnCreance() {
        List<LocataireCreanceDTO> result = echeanceLoyerRepository.locatairesEnCreance().stream()
                .map(row -> LocataireCreanceDTO.builder()
                        .idLocataire((Integer) row[0])
                        .nomComplet((String) row[1])
                        .nbEcheancesEnRetard(((Number) row[2]).longValue())
                        .montantDu(toBigDecimal(row[3]))
                        .build())
                .toList();

        return result;
    }

    public ResponseEntity<?> getBailleursCreanciers() {
        return buildSuccessResponse(HttpStatus.OK, "Bailleurs en attente de reversement", "BAILLEURS_CREANCIERS",
                cache.get("creanciers", this::calculerBailleursCreanciers));
    }

    private List<BailleurCreancierDTO> calculerBailleursCreanciers() {
        List<BailleurCreancierDTO> result = echeanceLoyerRepository.bailleursCreanciers().stream()
                .map(row -> BailleurCreancierDTO.builder()
                        .idBailleur((Integer) row[0])
                        .nomComplet((String) row[1])
                        .nbMois(((Number) row[2]).longValue())
                        .montantDu(toBigDecimal(row[3]))
                        .plusAncienneEcheance(row[4] instanceof java.sql.Date d ? d.toLocalDate() : (LocalDate) row[4])
                        .build())
                .toList();

        return result;
    }

    public ResponseEntity<?> getGainAgenceDuMois(LocalDate periode) {
        LocalDate debutMois = (periode != null ? periode : LocalDate.now()).withDayOfMonth(1);
        GainAgenceDTO result = cache.get("gain|" + debutMois,
                () -> financeQueries.gainDuMois(debutMois, DateUtils.nomMoisFrancais(debutMois) + " " + debutMois.getYear()));
        return buildSuccessResponse(HttpStatus.OK, "Gain de l'agence du mois", "GAIN_AGENCE_MOIS", result);
    }

    public ResponseEntity<?> getFinancesPeriode(LocalDate debut, LocalDate fin) {
        if (debut != null && fin != null && fin.isBefore(debut)) {
            return buildSuccessResponse(HttpStatus.BAD_REQUEST, "La date de fin précède la date de début", "PERIODE_INVALIDE", null);
        }
        return buildSuccessResponse(HttpStatus.OK, "Finances de la période", "FINANCES_PERIODE", cache.get("finances|" + debut + "|" + fin, () -> financeQueries.finances(debut, fin)));
    }

    public ResponseEntity<?> getActivitePeriode(LocalDate debut, LocalDate fin) {
        if (debut != null && fin != null && fin.isBefore(debut)) {
            return buildSuccessResponse(HttpStatus.BAD_REQUEST, "La date de fin précède la date de début", "PERIODE_INVALIDE", null);
        }
        return buildSuccessResponse(HttpStatus.OK, "Activité de la période", "ACTIVITE_PERIODE",
                cache.get("activite|" + debut + "|" + fin, () -> activiteQueries.activite(debut, fin)));
    }

    public ResponseEntity<?> getEvolutionMensuelle(int mois) {
        int nb = Math.max(3, Math.min(mois, 24));
        return buildSuccessResponse(HttpStatus.OK, "Évolution mensuelle", "EVOLUTION_MENSUELLE", cache.get("evolution|" + nb, () -> financeQueries.evolution(nb)));
    }

    private Map<String, Long> toMap(java.util.List<Object[]> rows) {
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return result;
    }

    private Map<String, Map<String, Long>> toMapImbrique(java.util.List<Object[]> rows) {
        Map<String, Map<String, Long>> result = new HashMap<>();
        for (Object[] row : rows) {
            String categorie = String.valueOf(row[0]);
            String disponibilite = String.valueOf(row[1]);
            long count = ((Number) row[2]).longValue();

            result.computeIfAbsent(categorie, k -> new HashMap<>())
                    .put(disponibilite, count);
        }
        return result;
    }

    private static BigDecimal toBigDecimal(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }

    private static BigDecimal defaultZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

}