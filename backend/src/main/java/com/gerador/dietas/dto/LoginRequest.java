package com.gerador.dietas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um e-mail válido")
        String email,

        @NotBlank(message = "é obrigatória")
        String password
) {
}
