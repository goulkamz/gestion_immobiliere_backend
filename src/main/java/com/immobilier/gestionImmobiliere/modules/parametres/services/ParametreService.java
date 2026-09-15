package com.immobilier.gestionImmobiliere.modules.parametres.services;

import com.immobilier.gestionImmobiliere.donnees.parametres.model.ParametreSysteme;
import com.immobilier.gestionImmobiliere.donnees.parametres.model.TypeValeurParametre;
import com.immobilier.gestionImmobiliere.donnees.parametres.repository.ParametreRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.parametres.dto.requests.UpdateParametreDTO;
import com.immobilier.gestionImmobiliere.modules.parametres.dto.responses.ParametreResponseDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class ParametreService {

    private final ParametreRepository parametreRepository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public ParametreService(ParametreRepository parametreRepository) {
        this.parametreRepository = parametreRepository;
    }

    @PostConstruct
    public void chargerCache() {
        parametreRepository.findAllByOrderByCleAsc()
                .forEach(p -> cache.put(p.getCle(), p.getValeur()));
    }

    public int getEntier(String cle, int valeurParDefaut) {
        String v = cache.get(cle);
        return v != null ? Integer.parseInt(v) : valeurParDefaut;
    }

    public BigDecimal getDecimal(String cle, BigDecimal valeurParDefaut) {
        String v = cache.get(cle);
        return v != null ? new BigDecimal(v) : valeurParDefaut;
    }

    public boolean getBooleen(String cle, boolean valeurParDefaut) {
        String v = cache.get(cle);
        return v != null ? Boolean.parseBoolean(v) : valeurParDefaut;
    }

    public String getTexte(String cle, String valeurParDefaut) {
        return cache.getOrDefault(cle, valeurParDefaut);
    }

    public ResponseEntity<?> getAll() {
        List<ParametreResponseDTO> result = parametreRepository.findAllByOrderByCleAsc().stream()
                .map(this::toDto).toList();
        return buildSuccessResponse(HttpStatus.OK, "Paramètres système", "PARAMETRES_LIST", result);
    }

    /**
     * Modifie la valeur d'un paramètre existant. La clé et le type ne sont
     * jamais modifiables (seule la valeur évolue) — évite qu'un paramètre
     * ENTIER ne devienne accidentellement TEXTE en cours de route.
     */
    @Transactional
    public ResponseEntity<?> mettreAJour(String cle, UpdateParametreDTO dto, Integer currentUserId) {
        ParametreSysteme parametre = parametreRepository.findByCle(cle)
                .orElseThrow(() -> new ResourceNotFoundException("paramètre", cle));

        validerFormat(parametre.getTypeValeur(), dto.getValeur());

        parametre.setValeur(dto.getValeur());
        parametre.setUserUpdate(currentUserId);
        parametreRepository.save(parametre);

        cache.put(cle, dto.getValeur()); // rafraîchi immédiatement, pas besoin de redémarrage

        return buildSuccessResponse(HttpStatus.OK, "Paramètre mis à jour", "PARAMETRE_UPDATED", toDto(parametre));
    }

    private void validerFormat(TypeValeurParametre type, String valeur) {
        try {
            switch (type) {
                case ENTIER -> Integer.parseInt(valeur);
                case DECIMAL -> new BigDecimal(valeur);
                case BOOLEEN -> {
                    if (!valeur.equalsIgnoreCase("true") && !valeur.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException();
                    }
                }
                case TEXTE -> { /* toujours valide */ }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Valeur invalide pour un paramètre de type " + type);
        }
    }

    private ParametreResponseDTO toDto(ParametreSysteme p) {
        return ParametreResponseDTO.builder()
                .idParametre(p.getIdParametre())
                .cle(p.getCle())
                .valeur(p.getValeur())
                .typeValeur(p.getTypeValeur())
                .description(p.getDescription())
                .build();
    }
}