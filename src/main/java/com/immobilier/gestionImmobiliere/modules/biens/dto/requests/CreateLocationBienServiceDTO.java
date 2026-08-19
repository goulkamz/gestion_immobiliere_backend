package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateLocationBienServiceDTO {
    @NotNull private Integer idBienService;
    private String destination;
    @NotNull private LocalDateTime dateDebut;
    @NotNull private LocalDateTime dateFin;
}