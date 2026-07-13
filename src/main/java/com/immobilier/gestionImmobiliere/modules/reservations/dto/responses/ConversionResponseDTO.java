package com.immobilier.gestionImmobiliere.modules.reservations.dto.responses;

import com.immobilier.gestionImmobiliere.modules.contrats.dto.responses.ContratLocationResponseDTO;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ConversionResponseDTO {
    private Integer idReservation;
    private ContratLocationResponseDTO contratLocation;
}