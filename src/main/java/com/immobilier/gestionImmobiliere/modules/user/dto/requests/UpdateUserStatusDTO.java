package com.immobilier.gestionImmobiliere.modules.user.dto.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusDTO {
    @NotNull private Boolean flagActif;
}