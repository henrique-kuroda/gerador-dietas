package com.gerador.dietas.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretGuardTest {

    private static final String DEV_FALLBACK =
            "changeme-this-is-a-very-long-secret-key-for-jwt-signing-min-256-bits";
    private static final String STRONG_SECRET =
            "um-segredo-forte-definido-via-env-var-com-mais-de-256-bits-aqui";

    @Test
    void abortaBootComSegredoDefaultForaDoProfileDev() {
        JwtSecretGuard guard = guard(DEV_FALLBACK, "prod");

        assertThatThrownBy(guard::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void permiteSegredoDefaultNoProfileDev() {
        JwtSecretGuard guard = guard(DEV_FALLBACK, "dev");

        assertThatCode(guard::verify).doesNotThrowAnyException();
    }

    @Test
    void permiteSegredoCustomizadoEmQualquerProfile() {
        JwtSecretGuard guard = guard(STRONG_SECRET, "prod");

        assertThatCode(guard::verify).doesNotThrowAnyException();
    }

    private JwtSecretGuard guard(String secret, String activeProfile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(activeProfile);
        return new JwtSecretGuard(new JwtProperties(secret, 3_600_000L), environment);
    }
}
