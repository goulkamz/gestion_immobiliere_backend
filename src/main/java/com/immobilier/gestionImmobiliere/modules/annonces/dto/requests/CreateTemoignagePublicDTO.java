package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.RoleTemoignage;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Depot public (sans compte), a valider avant publication : volontairement plus restreint que
 * CreateTemoignageDTO (pas de photoUrl ni de date, pas de role AGENCE, pas de HTML ni de lien).
 */
@Data
public class CreateTemoignagePublicDTO {

    private static final String SANS_HTML = "(?s)[^<>]*";
    private static final String SANS_LIEN = "(?is)(?!.*(https?:|www\\.|ftp:)).*";

    @NotBlank @Size(max = 100)
    @Pattern(regexp = SANS_HTML, message = "Les caractères < et > sont interdits")
    private String nomAuteur;

    @NotNull private RoleTemoignage role;

    @NotBlank @Size(min = 10, max = 1000)
    @Pattern(regexp = SANS_HTML, message = "Les caractères < et > sont interdits")
    @Pattern(regexp = SANS_LIEN, message = "Les liens sont interdits")
    private String texte;

    @Min(1) @Max(5) private Short note;

    // Piege a robots : champ cache dans le formulaire, un humain ne le remplit pas
    private String siteWeb;

    @JsonIgnore
    @AssertTrue(message = "Ce rôle ne peut pas être choisi")
    public boolean isRoleAutorise() {
        return role != RoleTemoignage.AGENCE;
    }
}
