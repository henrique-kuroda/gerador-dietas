package com.gerador.dietas.security;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Aborta o boot se o segredo JWT ainda for o fallback de desenvolvimento fora
 * do profile {@code dev}. Sem isso, um deploy sem a env var JWT_SECRET sobe
 * com segredo público — qualquer token seria forjável.
 */
@Component
public class JwtSecretGuard {

    static final String DEV_FALLBACK_PREFIX = "changeme";

    private final JwtProperties properties;
    private final Environment environment;

    public JwtSecretGuard(JwtProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void verify() {
        boolean devProfile = environment.acceptsProfiles(Profiles.of("dev"));
        if (!devProfile && properties.secret().startsWith(DEV_FALLBACK_PREFIX)) {
            throw new IllegalStateException(
                    "JWT_SECRET está com o valor default de desenvolvimento. "
                            + "Defina a env var JWT_SECRET com um segredo forte (>= 256 bits) "
                            + "ou ative o profile 'dev' para ambiente local.");
        }
    }
}
