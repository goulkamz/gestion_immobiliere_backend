package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateLocationBienServiceDTO {
    @NotNull private Integer idBienService;
    // Renseigné uniquement par un agent/admin qui loue pour le compte d'un client
    private Integer idClient;
    private String destination;
    @NotNull private LocalDateTime dateDebut;
    @NotNull private LocalDateTime dateFin;
}