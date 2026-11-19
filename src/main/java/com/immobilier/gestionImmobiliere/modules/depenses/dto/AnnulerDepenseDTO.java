package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnnulerDepenseDTO {
    @NotBlank @Size(max = 254) private String motif;
}
