package com.immobilier.gestionImmobiliere.modules.user.services;

import com.immobilier.gestionImmobiliere.donnees.user.model.ERole;
import com.immobilier.gestionImmobiliere.donnees.user.model.Role;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.RoleRepository;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.regex.Pattern;

/**
 * Cree le premier compte administrateur au demarrage, a partir des variables d'environnement
 * BOOTSTRAP_ADMIN_EMAIL et BOOTSTRAP_ADMIN_PASSWORD (fichier .env du serveur).
 * <p>
 * Necessaire en production : la base n'y contient aucun compte (le seed de demonstration n'est charge
 * qu'en dev et test) et l'interface ne permet pas de creer un admin. Ne fait rien si un admin actif existe
 * deja ou si les variables sont absentes : on peut donc laisser les variables en place, mais le mot de passe
 * n'a plus de raison d'y rester une fois le compte cree (a changer ensuite depuis l'application).
 */
@Component
public class AmorcageAdminService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AmorcageAdminService.class);

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    // Plus strict que la regle d'inscription (6 caracteres) : ce compte peut tout faire
    private static final Pattern MOT_DE_PASSE_FORT =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9])(?=\\S+$).{12,}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transaction;
    private final String email;
    private final String motDePasse;
    private final String nom;
    private final String prenom;

    public AmorcageAdminService(UserRepository userRepository, RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder, PlatformTransactionManager transactionManager,
                                @Value("${app.bootstrap.admin-email:}") String email,
                                @Value("${app.bootstrap.admin-password:}") String motDePasse,
                                @Value("${app.bootstrap.admin-nom:Administrateur}") String nom,
                                @Value("${app.bootstrap.admin-prenom:Principal}") String prenom) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.transaction = new TransactionTemplate(transactionManager);
        this.email = email;
        this.motDePasse = motDePasse;
        this.nom = nom;
        this.prenom = prenom;
    }

    @Override
    public void run(ApplicationArguments args) {
        amorcer(email, motDePasse, nom, prenom);
    }

    /** @return vrai si un compte administrateur a ete cree */
    public boolean amorcer(String emailBrut, String mdp, String nomAdmin, String prenomAdmin) {
        if (emailBrut == null || emailBrut.isBlank() || mdp == null || mdp.isBlank()) {
            return false;
        }
        if (userRepository.existsByRole_LibelleRoleAndIsDeletedFalse(ERole.ROLE_ADMIN)) {
            return false;
        }

        String adresse = emailBrut.trim().toLowerCase();
        if (!EMAIL.matcher(adresse).matches()) {
            log.error("Amorcage de l'admin ignore : BOOTSTRAP_ADMIN_EMAIL n'est pas une adresse e-mail valide");
            return false;
        }
        if (!MOT_DE_PASSE_FORT.matcher(mdp).matches()) {
            log.error("Amorcage de l'admin ignore : BOOTSTRAP_ADMIN_PASSWORD trop faible "
                    + "(12 caracteres minimum, avec majuscule, minuscule, chiffre et caractere special, sans espace)");
            return false;
        }
        if (userRepository.existsByEmail(adresse)) {
            log.error("Amorcage de l'admin ignore : l'adresse {} est deja utilisee par un autre compte", adresse);
            return false;
        }

        Role role = roleRepository.findByLibelleRole(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Le role ROLE_ADMIN est absent de la base"));

        transaction.executeWithoutResult(status -> {
            User admin = new User();
            admin.setEmail(adresse);
            admin.setPassword(passwordEncoder.encode(mdp));
            admin.setNom(nomAdmin);
            admin.setPrenom(prenomAdmin);
            admin.setRole(role);
            admin.setFlagActif(true);
            admin.initTimestamp();
            userRepository.save(admin);
        });
        // Jamais le mot de passe dans les journaux
        log.warn("Compte administrateur initial cree : {}. Changez son mot de passe depuis l'application "
                + "puis retirez BOOTSTRAP_ADMIN_PASSWORD du fichier .env", adresse);
        return true;
    }
}
