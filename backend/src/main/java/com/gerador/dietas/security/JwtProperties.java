package com.gerador.dietas.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(

        @NotBlank
        String secret,

        @Positive
        long expirationMs
) {
}
