package com.immobilier.gestionImmobiliere.modules.annonces.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.RoleTemoignage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateTemoignageDTO {
    @NotBlank @Size(max = 254) private String nomAuteur;
    @NotNull private RoleTemoignage role;
    @NotBlank private String texte;
    @Min(1) @Max(5) private Short note;
    @Size(max = 512) private String photoUrl;
    // Optionnelle : date du jour par defaut
    private LocalDate date;
}
