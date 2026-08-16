package com.gerador.dietas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DietAdjustRequest(
        @NotBlank(message = "descreva o ajuste desejado")
        @Size(max = 500, message = "deve ter no máximo 500 caracteres")
        String instruction
) {
}
