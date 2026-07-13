package com.immobilier.gestionImmobiliere.modules.user.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.user.model.ERole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleDTO {
    @NotNull private ERole role;
}