package com.gerador.dietas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "é obrigatório")
        @Size(max = 120, message = "deve ter no máximo 120 caracteres")
        String name,

        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um e-mail válido")
        @Size(max = 255)
        String email,

        @NotBlank(message = "é obrigatória")
        @Size(min = 8, max = 100, message = "deve ter entre 8 e 100 caracteres")
        String password
) {
}
