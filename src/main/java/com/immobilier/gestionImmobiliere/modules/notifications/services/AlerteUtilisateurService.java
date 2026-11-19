package com.immobilier.gestionImmobiliere.modules.notifications.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.Avenant;
import com.immobilier.gestionImmobiliere.donnees.biens.model.LocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.LocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.notifications.model.TypeNotification;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.DecompteSortie;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.Paiement;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.reservations.model.ReservationMaison;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Alertes in-app destinees aux clients et aux bailleurs (tranche 3). Le destinataire est toujours deduit des donnees
// metier (locataire, client, proprietaire de la cour), jamais d'un identifiant venu de la requete.
@Service
public class AlerteUtilisateurService {

    private static final Logger log = LoggerFactory.getLogger(AlerteUtilisateurService.class);
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int JOURS_PREAVIS_ECHEANCE_LOYER = 3;
    private static final int JOURS_PREAVIS_LOCATION = 2;
    private static final int JOURS_PREAVIS_MANDAT = 30;

    private static final String LIEN_RESERVATIONS = "/espace-client/reservations";
    private static final String LIEN_LOCATIONS = "/espace-client/locations";
    private static final String LIEN_ECHEANCES = "/espace-client/echeances";
    private static final String LIEN_RECUS = "/espace-client/recus";
    private static final String LIEN_REMBOURSEMENTS = "/espace-client/remboursements";
    private static final String LIEN_BAILLEUR_MANDATS = "/espace-bailleur/mandats";
    private static final String LIEN_CONTRATS = "/espace-client/contrats";
    private static final String LIEN_BAILLEUR_CONTRATS = "/espace-bailleur/contrats";
    private static final String LIEN_BAILLEUR_REVENUS = "/espace-bailleur/revenus";
    private static final String LIEN_BAILLEUR_DEPENSES = "/espace-bailleur/depenses";
    private static final String LIEN_BAILLEUR_DEVIS = "/espace-bailleur/devis";

    private final NotificationInAppService notificationService;
    private final EcheanceLoyerRepository echeanceRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final ContratMandatRepository contratMandatRepository;
    private final LocationBienServiceRepository locationRepository;
    private final EmailAlerteService emailService;

    public AlerteUtilisateurService(NotificationInAppService notificationService, EcheanceLoyerRepository echeanceRepository,
                                    ContratLocationRepository contratLocationRepository, ContratMandatRepository contratMandatRepository,
                                    LocationBienServiceRepository locationRepository, EmailAlerteService emailService) {
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.echeanceRepository = echeanceRepository;
        this.contratLocationRepository = contratLocationRepository;
        this.contratMandatRepository = contratMandatRepository;
        this.locationRepository = locationRepository;
    }

    // ---- Evenements declenches par une action (une alerte ne doit jamais faire echouer l'action metier) ----

    public void reservationConfirmee(ReservationMaison r) {
        reservation(r, "RESERVATION_CONFIRMEE", "Réservation confirmée",
                "Votre réservation de " + r.getMaison().getNomCommunMaison() + " est confirmée.");
    }

    public void reservationAnnulee(ReservationMaison r, boolean expiree) {
        reservation(r, "RESERVATION_ANNULEE", expiree ? "Réservation expirée" : "Réservation annulée",
                "Votre réservation de " + r.getMaison().getNomCommunMaison()
                        + (expiree ? " a expiré faute de confirmation." : " a été annulée."));
    }

    private void reservation(ReservationMaison r, String prefixeCle, String titre, String message) {
        envoyer(r.getUser(), false, TypeNotification.DEMANDE, titre, message, cible(LIEN_RESERVATIONS, r.getIdReservation()),
                prefixeCle + ":" + r.getIdReservation());
    }

    public void locationConfirmee(LocationBienService l) {
        location(l, "LOCATION_CONFIRMEE", "Location confirmée", "Votre location de " + l.getBienService().getLibelle()
                + " est confirmée du " + l.getDateDebut().format(FORMAT_DATE) + " au " + l.getDateFin().format(FORMAT_DATE) + ".");
    }

    public void locationAnnulee(LocationBienService l) {
        location(l, "LOCATION_ANNULEE", "Demande de location annulée",
                "Votre demande de location de " + l.getBienService().getLibelle() + " a été annulée.");
    }

    public void locationTerminee(LocationBienService l) {
        location(l, "LOCATION_TERMINEE", "Location terminée",
                "Votre location de " + l.getBienService().getLibelle() + " est terminée.");
    }

    private void location(LocationBienService l, String prefixeCle, String titre, String message) {
        envoyer(l.getClient(), false, TypeNotification.DEMANDE, titre, message, cible(LIEN_LOCATIONS, l.getIdLocationBienService()),
                prefixeCle + ":" + l.getIdLocationBienService());
    }

    public void remboursementEffectue(LocationBienService l, Integer idRemboursement, BigDecimal montant) {
        envoyer(l.getClient(), true, TypeNotification.INFO, "Remboursement effectué",
                "Un remboursement de " + montant.stripTrailingZeros().toPlainString() + " vous a été versé pour la location de "
                        + l.getBienService().getLibelle() + ".", cible(LIEN_REMBOURSEMENTS, idRemboursement), "REMBOURSEMENT:" + idRemboursement);
    }

    // ---- Recus de paiement : le client est prevenu des qu'un paiement qu'il a effectue est enregistre ----

    // Loyer encaisse par l'agent (une ou plusieurs echeances d'un meme locataire)
    public void paiementLoyerRecu(Paiement p, List<Integer> idEcheances) {
        try {
            if (idEcheances == null || idEcheances.isEmpty()) return;
            EcheanceLoyer premiere = echeanceRepository.findById(idEcheances.get(0)).orElse(null);
            if (premiere == null || premiere.getEntiteEcheanceType() != TypeEcheance.LOCATION) return;
            ContratLocation c = contratLocationRepository.findById(premiere.getEntiteEcheanceId()).orElse(null);
            if (c == null) return;
            recu(c.getLocataire(), p, "le loyer de " + c.getMaison().getNomCommunMaison(), cible(LIEN_ECHEANCES, premiere.getIdEcheance()));
        } catch (RuntimeException e) {
            log.warn("Alerte de recu de loyer non creee : {}", e.getMessage());
        }
    }

    // Paiement a la confirmation d'une location de bien/service, ou complement de prolongation
    public void paiementLocationRecu(LocationBienService l, Paiement p, boolean prolongation) {
        recu(l.getClient(), p, (prolongation ? "la prolongation de " : "la location de ") + l.getBienService().getLibelle(), cible(LIEN_RECUS, p.getIdPaiement()));
    }

    // Montant manquant regle par le locataire a la sortie (le remboursement dans l'autre sens a sa propre alerte)
    public void paiementSortieRecu(DecompteSortie d, Paiement p) {
        ContratLocation c = d.getContratLocation();
        recu(c.getLocataire(), p, "le solde de sortie de " + c.getMaison().getNomCommunMaison(), cible(LIEN_CONTRATS, c.getIdContratLocation()));
    }

    // Trop-percu rendu au locataire a la sortie (decompte de sortie regle par l'agent)
    public void remboursementSortie(DecompteSortie d, Paiement p) {
        ContratLocation c = d.getContratLocation();
        if (c.getLocataire() == null || p == null || p.getMontantPaiement() == null || p.getMontantPaiement().signum() <= 0) return;
        envoyer(c.getLocataire(), true, TypeNotification.INFO, "Remboursement effectué",
                "Un remboursement de " + p.getMontantPaiement().stripTrailingZeros().toPlainString() + " vous a été versé pour votre sortie de "
                        + c.getMaison().getNomCommunMaison() + ".", cible(LIEN_CONTRATS, c.getIdContratLocation()), "REMBOURSEMENT_SORTIE:" + p.getIdPaiement());
    }

    private void recu(User client, Paiement p, String objet, String lien) {
        if (client == null || p == null || p.getMontantPaiement() == null || p.getMontantPaiement().signum() <= 0) return;
        envoyer(client, true, TypeNotification.INFO, "Paiement reçu",
                "Nous avons enregistré votre paiement de " + p.getMontantPaiement().stripTrailingZeros().toPlainString() + " pour " + objet
                        + ". Votre reçu est disponible dans « Mes reçus de paiement ».", lien, "PAIEMENT_RECU:" + p.getIdPaiement());
    }

    // Avenant ajoute a un contrat actif : le locataire et le bailleur concernes sont informes (document a consulter)
    public void avenantCree(Avenant a) {
        String cle = "AVENANT:" + a.getIdAvenant();
        String effet = a.getDateEffet().format(FORMAT_DATE);
        if (a.getContratLocation() != null) {
            ContratLocation c = a.getContratLocation();
            String message = "Un avenant n°" + a.getNumeroOrdre() + " a été ajouté au contrat de location de "
                    + c.getMaison().getNomCommunMaison() + ", à compter du " + effet + ".";
            envoyer(c.getLocataire(), false, TypeNotification.INFO, "Avenant à votre contrat", message, cible(LIEN_CONTRATS, c.getIdContratLocation()), cle);
            User bailleur = c.getMaison().getCour().getProprietaire();
            if (bailleur != null) {
                envoyer(bailleur, false, TypeNotification.INFO, "Avenant à un contrat de location", message, cible(LIEN_BAILLEUR_CONTRATS, c.getIdContratLocation()), cle);
            }
        } else if (a.getMandat() != null) {
            ContratMandat m = a.getMandat();
            User bailleur = m.getCour().getProprietaire();
            if (bailleur != null) {
                envoyer(bailleur, false, TypeNotification.INFO, "Avenant à votre mandat",
                        "Un avenant n°" + a.getNumeroOrdre() + " a été ajouté à votre mandat sur la cour "
                                + m.getCour().getReferenceCour() + ", à compter du " + effet + ".", cible(LIEN_BAILLEUR_MANDATS, m.getIdMandat()), cle);
            }
        }
    }

    // Mandat active : le bailleur sait que la gestion de sa cour commence (ses maisons disponibles sont publiees)
    public void mandatActive(ContratMandat m) {
        User bailleur = m.getCour().getProprietaire();
        if (bailleur == null) return;
        envoyer(bailleur, false, TypeNotification.INFO, "Mandat activé",
                "Votre mandat de gestion sur la cour " + m.getCour().getReferenceCour()
                        + " est actif : vos maisons disponibles sont désormais publiées sur le site.",
                cible(LIEN_BAILLEUR_MANDATS, m.getIdMandat()), "MANDAT_ACTIVE:" + m.getIdMandat());
    }

    // Mandat resilie par l'agent : le bailleur est informe, avec le motif saisi
    public void mandatResilie(ContratMandat m) {
        User bailleur = m.getCour().getProprietaire();
        if (bailleur == null) return;
        String motif = m.getMotifResiliation() == null || m.getMotifResiliation().isBlank() ? "" : " Motif : " + m.getMotifResiliation() + ".";
        envoyer(bailleur, false, TypeNotification.INFO, "Mandat résilié",
                "Votre mandat sur la cour " + m.getCour().getReferenceCour() + " a été résilié." + motif,
                cible(LIEN_BAILLEUR_MANDATS, m.getIdMandat()), "MANDAT_RESILIE:" + m.getIdMandat());
    }

    // Virement du loyer reverse au bailleur (confirme par l'agent)
    public void virementMandatConfirme(EcheanceLoyer echeance) {
        try {
            ContratMandat mandat = contratMandatRepository.findById(echeance.getEntiteEcheanceId()).orElse(null);
            if (mandat == null || mandat.getCour().getProprietaire() == null) return;
            String message = "Un virement de " + echeance.getMontantDu().stripTrailingZeros().toPlainString()
                    + " vous a été effectué (échéance du " + echeance.getDateEcheance().format(FORMAT_DATE) + ").";
            notificationService.creer(mandat.getCour().getProprietaire().getIdUser(), TypeNotification.INFO, "Virement effectué",
                    message, cible(LIEN_BAILLEUR_REVENUS, echeance.getIdEcheance()), "VIREMENT_MANDAT:" + echeance.getIdEcheance());
        } catch (RuntimeException e) {
            log.warn("Alerte de virement bailleur non creee : {}", e.getMessage());
        }
    }

    // Montant a reverser calcule par l'agent : le bailleur sait qu'un virement est a venir
    public void reversementCalcule(ContratMandat mandat, BigDecimal montantNet, String periode, String cle) {
        User bailleur = mandat.getCour().getProprietaire();
        if (bailleur == null) return;
        envoyer(bailleur, false, TypeNotification.INFO, "Montant à reverser calculé",
                "Le montant à vous reverser pour " + periode + " est de " + montantNet.stripTrailingZeros().toPlainString()
                        + " (cour " + mandat.getCour().getReferenceCour() + ").",
                cible(LIEN_BAILLEUR_REVENUS, idDepuisCle(cle)), cle);
    }

    // ---- Dépenses à la charge du bailleur ----

    public void depenseEnregistree(com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseBien d) {
        User bailleur = d.getCour().getProprietaire();
        if (bailleur == null) return;
        String etalement = d.getNombreMois() > 1 ? "en " + d.getNombreMois() + " fois" : "en une fois";
        envoyer(bailleur, false, TypeNotification.INFO, "Dépense enregistrée sur votre bien",
                "Une dépense de " + d.getMontant().stripTrailingZeros().toPlainString() + " (" + d.getLibelle()
                        + ") a été enregistrée sur la cour " + d.getCour().getReferenceCour()
                        + ". Elle sera retenue " + etalement + " sur vos reversements."
                        + (d.getAccordMotif() != null
                                ? " L'agence indique avoir obtenu votre accord (" + d.getAccordMotif()
                                        + "). Si ce n'est pas le cas, contactez l'agence."
                                : ""),
                cible(LIEN_BAILLEUR_DEPENSES, d.getIdDepense()), "DEPENSE:" + d.getIdDepense());
    }

    public void depenseAnnulee(com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseBien d) {
        User bailleur = d.getCour().getProprietaire();
        if (bailleur == null) return;
        envoyer(bailleur, false, TypeNotification.INFO, "Dépense annulée",
                "La dépense de " + d.getMontant().stripTrailingZeros().toPlainString() + " (" + d.getLibelle()
                        + ") sur la cour " + d.getCour().getReferenceCour() + " a été annulée : rien ne sera retenu.",
                cible(LIEN_BAILLEUR_DEPENSES, d.getIdDepense()), "DEPENSE_ANNULEE:" + d.getIdDepense());
    }

    public void depenseReechelonnee(com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseBien d, int anciensMois, int nouveauxMois) {
        User bailleur = d.getCour().getProprietaire();
        if (bailleur == null) return;
        BigDecimal solde = d.getMontant().subtract(d.getMontantImpute()).subtract(d.getMontantRegle()).subtract(d.getMontantAbandonne());
        envoyer(bailleur, false, TypeNotification.INFO, "Étalement d'une dépense modifié",
                "Le solde de " + solde.stripTrailingZeros().toPlainString() + " de la dépense « " + d.getLibelle()
                        + " » (cour " + d.getCour().getReferenceCour() + ") sera retenu sur " + nouveauxMois
                        + " reversement(s) au lieu de " + anciensMois + ". Motif : " + d.getMotifReechelonnement(),
                cible(LIEN_BAILLEUR_DEPENSES, d.getIdDepense()), "DEPENSE_REECHELONNEE:" + d.getIdDepense() + ":" + d.getDateReechelonnement());
    }

    // Résiliation du mandat avec des dépenses non soldées : ce qui a été retenu d'office et ce qui reste dû
    public void depensesApresResiliation(ContratMandat mandat, BigDecimal retenu, BigDecimal restant) {
        User bailleur = mandat.getCour().getProprietaire();
        if (bailleur == null) return;
        StringBuilder message = new StringBuilder("Suite à la résiliation du mandat de la cour ")
                .append(mandat.getCour().getReferenceCour()).append(", l'étalement des dépenses avancées par l'agence s'arrête.");
        if (retenu.signum() > 0) {
            message.append(" ").append(retenu.stripTrailingZeros().toPlainString())
                    .append(" ont été retenus sur vos reversements en attente.");
        }
        message.append(restant.signum() > 0
                ? " Il reste " + restant.stripTrailingZeros().toPlainString()
                        + " à régler : ce solde sera retenu sur vos derniers reversements, ou à défaut réglé directement à l'agence."
                : " Elles sont entièrement soldées.");
        envoyer(bailleur, false, TypeNotification.INFO, "Dépenses à solder après la résiliation", message.toString(),
                LIEN_BAILLEUR_DEPENSES, "DEPENSES_RESILIATION:" + mandat.getIdMandat());
    }

    public void depenseReglee(com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseBien d, BigDecimal montant) {
        User bailleur = d.getCour().getProprietaire();
        if (bailleur == null) return;
        BigDecimal solde = d.getMontant().subtract(d.getMontantImpute()).subtract(d.getMontantRegle()).subtract(d.getMontantAbandonne());
        envoyer(bailleur, false, TypeNotification.INFO, "Règlement enregistré",
                "L'agence a enregistré votre règlement de " + montant.stripTrailingZeros().toPlainString() + " pour la dépense « "
                        + d.getLibelle() + " » (cour " + d.getCour().getReferenceCour() + "). "
                        + (solde.signum() > 0 ? "Reste dû : " + solde.stripTrailingZeros().toPlainString() + "." : "Elle est soldée."),
                cible(LIEN_BAILLEUR_DEPENSES, d.getIdDepense()),
                "DEPENSE_REGLEMENT:" + d.getIdDepense() + ":" + d.getMontantRegle().stripTrailingZeros().toPlainString());
    }

    // Règlement saisi par erreur puis annulé : le bailleur doit savoir que sa dette a été corrigée
    public void reglementAnnule(com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseBien d,
                                com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseReglement r) {
        User bailleur = d.getCour().getProprietaire();
        if (bailleur == null) return;
        BigDecimal solde = d.getMontant().subtract(d.getMontantImpute()).subtract(d.getMontantRegle()).subtract(d.getMontantAbandonne());
        envoyer(bailleur, false, TypeNotification.INFO, "Règlement annulé",
                "Le règlement de " + r.getMontant().stripTrailingZeros().toPlainString() + " enregistré pour la dépense « " + d.getLibelle()
                        + " » (cour " + d.getCour().getReferenceCour() + ") a été annulé : " + r.getMotifAnnulation() + ". "
                        + (solde.signum() > 0 ? "Reste dû : " + solde.stripTrailingZeros().toPlainString() + "." : "Elle reste soldée."),
                cible(LIEN_BAILLEUR_DEPENSES, d.getIdDepense()), "DEPENSE_REGLEMENT_ANNULE:" + r.getIdReglement());
    }

    // Abandon de créance : l'agence renonce à réclamer le reste dû
    public void creanceAbandonnee(com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseBien d, BigDecimal montant) {
        User bailleur = d.getCour().getProprietaire();
        if (bailleur == null) return;
        envoyer(bailleur, false, TypeNotification.INFO, "Reste dû abandonné par l'agence",
                "L'agence renonce à vous réclamer " + montant.stripTrailingZeros().toPlainString() + " pour la dépense « " + d.getLibelle()
                        + " » (cour " + d.getCour().getReferenceCour() + "). Elle est soldée. Motif : " + d.getMotifAbandon(),
                cible(LIEN_BAILLEUR_DEPENSES, d.getIdDepense()), "DEPENSE_ABANDON:" + d.getIdDepense());
    }

    // Creance abandonnee puis retablie par l'admin : le reste redevient du
    public void creanceRetablie(com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseBien d, BigDecimal montant) {
        User bailleur = d.getCour().getProprietaire();
        if (bailleur == null) return;
        envoyer(bailleur, false, TypeNotification.INFO, "Reste dû rétabli",
                "L'abandon de " + montant.stripTrailingZeros().toPlainString() + " sur la dépense « " + d.getLibelle() + " » (cour "
                        + d.getCour().getReferenceCour() + ") est annulé : ce montant redevient dû à l'agence. Motif : " + d.getMotifRetablissement(),
                cible(LIEN_BAILLEUR_DEPENSES, d.getIdDepense()), "DEPENSE_RETABLIE:" + d.getIdDepense() + ":" + d.getDateRetablissement());
    }

    // L'agence a repondu a la contestation du bailleur
    public void contestationRepondue(com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseBien d) {
        User bailleur = d.getCour().getProprietaire();
        if (bailleur == null) return;
        envoyer(bailleur, false, TypeNotification.INFO, "Réponse à votre contestation",
                "L'agence a répondu à votre contestation de la dépense « " + d.getLibelle() + " » (cour " + d.getCour().getReferenceCour()
                        + ") : " + d.getReponseContestation(),
                cible(LIEN_BAILLEUR_DEPENSES, d.getIdDepense()), "DEPENSE_CONTESTATION_REPONSE:" + d.getIdDepense());
    }

    // ---- Devis de travaux ----

    // Accord donné hors plateforme et enregistré par l'agent : le bailleur peut le contester tant que la dépense n'existe pas
    public void devisAccordHorsPlateforme(com.immobilier.gestionImmobiliere.donnees.depenses.model.DevisTravaux d) {
        User bailleur = d.getCour().getProprietaire();
        if (bailleur == null) return;
        envoyer(bailleur, false, TypeNotification.DEMANDE, "Accord enregistré sur un devis",
                "L'agence a enregistré votre accord, donné hors plateforme le " + d.getAccordDate().format(FORMAT_DATE)
                        + ", sur le devis de " + d.getMontant().stripTrailingZeros().toPlainString() + " (" + d.getLibelle()
                        + ") pour la cour " + d.getCour().getReferenceCour() + " : " + d.getAccordMotif()
                        + ". Si vous ne le reconnaissez pas, contestez-le depuis vos devis avant que la dépense soit enregistrée.",
                cible(LIEN_BAILLEUR_DEVIS, d.getIdDevis()), "DEVIS_ACCORD_HORS_PLATEFORME:" + d.getIdDevis() + ":" + d.getNombreSoumissions());
    }

    // Rappel avant l'expiration d'un devis resté sans réponse (une fois par soumission)
    public boolean devisRappel(com.immobilier.gestionImmobiliere.donnees.depenses.model.DevisTravaux d, java.time.LocalDateTime dateLimite) {
        User bailleur = d.getCour().getProprietaire();
        if (bailleur == null) return false;
        return envoyer(bailleur, false, TypeNotification.DEMANDE, "Devis en attente de votre réponse",
                "Le devis de " + d.getMontant().stripTrailingZeros().toPlainString() + " (" + d.getLibelle() + ") pour la cour "
                        + d.getCour().getReferenceCour() + " attend votre réponse. Sans réponse avant le "
                        + dateLimite.format(FORMAT_DATE) + ", il expirera.",
                cible(LIEN_BAILLEUR_DEVIS, d.getIdDevis()), "DEVIS_RAPPEL:" + d.getIdDevis() + ":" + d.getNombreSoumissions());
    }

    public void devisExpire(com.immobilier.gestionImmobiliere.donnees.depenses.model.DevisTravaux d) {
        User bailleur = d.getCour().getProprietaire();
        if (bailleur == null) return;
        envoyer(bailleur, false, TypeNotification.INFO, "Devis expiré",
                "Le devis de " + d.getMontant().stripTrailingZeros().toPlainString() + " (" + d.getLibelle() + ") pour la cour "
                        + d.getCour().getReferenceCour() + " est resté sans réponse : il a expiré. L'agence peut vous le soumettre à nouveau.",
                cible(LIEN_BAILLEUR_DEVIS, d.getIdDevis()), "DEVIS_EXPIRE:" + d.getIdDevis() + ":" + d.getNombreSoumissions());
    }

    // Devis soumis (ou soumis a nouveau apres renegociation) : le bailleur doit repondre
    public void devisSoumis(com.immobilier.gestionImmobiliere.donnees.depenses.model.DevisTravaux d) {
        User bailleur = d.getCour().getProprietaire();
        if (bailleur == null) return;
        boolean revise = d.getNombreSoumissions() > 1;
        envoyer(bailleur, false, TypeNotification.DEMANDE, revise ? "Devis révisé à valider" : "Devis de travaux à valider",
                "L'agence vous soumet " + (revise ? "une nouvelle version du devis" : "un devis") + " de "
                        + d.getMontant().stripTrailingZeros().toPlainString() + " (" + d.getLibelle()
                        + ") pour la cour " + d.getCour().getReferenceCour() + ". Votre réponse est attendue.",
                cible(LIEN_BAILLEUR_DEVIS, d.getIdDevis()), "DEVIS_SOUMIS:" + d.getIdDevis() + ":" + d.getNombreSoumissions());
    }

    public void devisAnnule(com.immobilier.gestionImmobiliere.donnees.depenses.model.DevisTravaux d) {
        User bailleur = d.getCour().getProprietaire();
        if (bailleur == null) return;
        envoyer(bailleur, false, TypeNotification.INFO, "Devis retiré par l'agence",
                "Le devis de " + d.getMontant().stripTrailingZeros().toPlainString() + " (" + d.getLibelle()
                        + ") pour la cour " + d.getCour().getReferenceCour() + " a été retiré : aucune dépense ne sera engagée.",
                cible(LIEN_BAILLEUR_DEVIS, d.getIdDevis()), "DEVIS_ANNULE:" + d.getIdDevis());
    }

    // ---- Job quotidien ----

    /**
     * Echeances de loyer proches ou en retard (client locataire) et locations de biens/services qui se
     * terminent bientot (client). Cles anti-doublons : une alerte par echeance et par etat, une par date de fin.
     */
    @Transactional
    public void alerterClients() {
        LocalDate aujourdhui = LocalDate.now();
        int creees = 0;

        // Echeances a venir (ou depassees mais encore dans la tolerance) : EN_ATTENTE
        List<EcheanceLoyer> proches = echeanceRepository.findByEntiteEcheanceTypeAndStatutAndDateEcheanceBefore(
                TypeEcheance.LOCATION, StatutEcheance.EN_ATTENTE, aujourdhui.plusDays(JOURS_PREAVIS_ECHEANCE_LOYER + 1L));
        for (EcheanceLoyer e : proches) {
            ContratLocation contrat = contratLocationRepository.findById(e.getEntiteEcheanceId()).orElse(null);
            if (contrat == null) continue;
            String quand = e.getDateEcheance().isBefore(aujourdhui) ? "était attendu le " : "est attendu le ";
            if (envoyer(contrat.getLocataire(), true, TypeNotification.ECHEANCE, "Loyer à payer",
                    "Votre loyer de " + contrat.getMaison().getNomCommunMaison() + " " + quand + e.getDateEcheance().format(FORMAT_DATE) + ".",
                    cible(LIEN_ECHEANCES, e.getIdEcheance()), "ECHEANCE_PROCHE:" + e.getIdEcheance())) creees++;
        }

        // Echeances en retard
        List<EcheanceLoyer> enRetard = echeanceRepository.findByEntiteEcheanceTypeAndStatutAndDateEcheanceBefore(
                TypeEcheance.LOCATION, StatutEcheance.EN_RETARD, aujourdhui);
        for (EcheanceLoyer e : enRetard) {
            ContratLocation contrat = contratLocationRepository.findById(e.getEntiteEcheanceId()).orElse(null);
            if (contrat == null) continue;
            if (envoyer(contrat.getLocataire(), true, TypeNotification.ECHEANCE, "Loyer en retard",
                    "Votre loyer de " + contrat.getMaison().getNomCommunMaison() + " du " + e.getDateEcheance().format(FORMAT_DATE)
                            + " est en retard.", cible(LIEN_ECHEANCES, e.getIdEcheance()), "ECHEANCE_RETARD:" + e.getIdEcheance())) creees++;
        }

        // Fin de location de bien/service
        List<LocationBienService> locations = locationRepository.findByStatutAndDateFinBefore(
                StatutLocationBienService.ACTIF, aujourdhui.plusDays(JOURS_PREAVIS_LOCATION + 1L).atStartOfDay());
        for (LocationBienService l : locations) {
            LocalDate fin = l.getDateFin().toLocalDate();
            boolean depassee = fin.isBefore(aujourdhui);
            String message = "Votre location de " + l.getBienService().getLibelle() + (depassee ? " est terminée depuis le " : " se termine le ")
                    + fin.format(FORMAT_DATE) + ".";
            if (envoyer(l.getClient(), true, TypeNotification.ECHEANCE, depassee ? "Location terminée" : "Fin de location proche",
                    message, cible(LIEN_LOCATIONS, l.getIdLocationBienService()), "CLIENT_LOCATION_FIN:" + l.getIdLocationBienService() + ":" + fin)) creees++;
        }
        creees += alerterMandatsBailleurs(aujourdhui);
        if (creees > 0) log.info("Alertes client : {} notification(s) creee(s)", creees);
    }

    // Mandat ACTIF qui se termine dans les JOURS_PREAVIS_MANDAT prochains jours : alerte le bailleur concerne
    private int alerterMandatsBailleurs(LocalDate aujourdhui) {
        int creees = 0;
        for (ContratMandat m : contratMandatRepository.findByStatut(StatutMandat.ACTIF)) {
            if (m.getDateFin() == null || m.getCour().getProprietaire() == null) continue;
            LocalDate fin = m.getDateFin().toLocalDate();
            if (fin.isAfter(aujourdhui.plusDays(JOURS_PREAVIS_MANDAT))) continue;
            boolean depasse = fin.isBefore(aujourdhui);
            if (envoyer(m.getCour().getProprietaire(), false, TypeNotification.ECHEANCE,
                    depasse ? "Mandat arrivé à terme" : "Mandat bientôt terminé",
                    "Votre mandat sur la cour " + m.getCour().getReferenceCour() + (depasse ? " est arrivé à terme le " : " se termine le ")
                            + fin.format(FORMAT_DATE) + ".", cible(LIEN_BAILLEUR_MANDATS, m.getIdMandat()), "MANDAT_FIN:" + m.getIdMandat() + ":" + fin)) creees++;
        }
        return creees;
    }

    // Identifiant porte par la cle d'alerte (« REVERSEMENT_CALCULE:12 » -> 12)
    private static Integer idDepuisCle(String cle) {
        return Integer.valueOf(cle.substring(cle.indexOf(':') + 1));
    }

    // Lien vers la page, avec l'identifiant de la ligne concernee (la page la repere et la met en evidence)
    private static String cible(String lien, Integer id) {
        return id == null ? lien : lien + "?cible=" + id;
    }

    // Mail envoye uniquement si la notification vient d'etre creee (pas de renvoi sur une cle deja connue)
    private boolean envoyer(User u, boolean email, TypeNotification type, String titre, String message, String lien, String cle) {
        try {
            boolean creee = notificationService.creer(u.getIdUser(), type, titre, message, lien, cle);
            if (creee && email) emailService.envoyer(u, titre, message);
            return creee;
        } catch (RuntimeException e) {
            log.warn("Alerte {} non creee : {}", cle, e.getMessage());
            return false;
        }
    }
}
