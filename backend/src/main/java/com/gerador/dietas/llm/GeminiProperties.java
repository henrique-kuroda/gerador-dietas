package com.gerador.dietas.llm;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm.gemini")
public record GeminiProperties(

        String apiKey,

        @NotBlank
        String model,

        @NotBlank
        String baseUrl
) {
}
