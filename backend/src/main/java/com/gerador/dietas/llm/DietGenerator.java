package com.gerador.dietas.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.llm.LlmException.Kind;
import com.gerador.dietas.metabolism.MetabolismResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class DietGenerator {

    private static final Logger log = LoggerFactory.getLogger(DietGenerator.class);

    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final String promptTemplate;

    public DietGenerator(LlmService llmService,
                         ObjectMapper objectMapper,
                         @Value("classpath:prompts/diet-prompt.txt") Resource promptResource) throws IOException {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public DietGeneratorResult generate(Profile profile, MetabolismResult metabolism) {
        String prompt = buildPrompt(profile, metabolism);
        String raw = llmService.generateJson(prompt);
        String cleaned = stripCodeFences(raw);
        DietContent content = parse(cleaned);
        return new DietGeneratorResult(content, prompt);
    }

    String buildPrompt(Profile profile, MetabolismResult metabolism) {
        String restrictions = profile.getDietaryRestrictions();
        return promptTemplate
                .replace("{sex}", profile.getSex().name())
                .replace("{age}", String.valueOf(profile.getAge()))
                .replace("{weightKg}", trimDouble(profile.getWeightKg()))
                .replace("{heightCm}", trimDouble(profile.getHeightCm()))
                .replace("{activityLevel}", profile.getActivityLevel().name())
                .replace("{goal}", profile.getGoal().name())
                .replace("{dietaryRestrictions}", (restrictions == null || restrictions.isBlank()) ? "nenhuma" : restrictions)
                .replace("{mealsPerDay}", String.valueOf(profile.getMealsPerDay()))
                .replace("{targetCalories}", String.valueOf(metabolism.targetCalories()));
    }

    /**
     * Remove cercas de código markdown que a LLM eventualmente adiciona
     * (```json ... ``` ou ``` ... ```).
     */
    static String stripCodeFences(String raw) {
        String text = raw.trim();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        if (firstNewline > 0) {
            text = text.substring(firstNewline + 1);
        } else {
            text = text.substring(3);
        }
        int closing = text.lastIndexOf("```");
        if (closing >= 0) {
            text = text.substring(0, closing);
        }
        return text.trim();
    }

    private DietContent parse(String json) {
        try {
            DietContent content = objectMapper.readValue(json, DietContent.class);
            if (content.meals() == null || content.meals().isEmpty()) {
                throw new LlmException(Kind.INVALID_RESPONSE, "Plano alimentar veio sem refeições");
            }
            return content;
        } catch (JsonProcessingException ex) {
            log.warn("Falha ao parsear JSON da LLM. Conteúdo bruto:\n{}", json);
            throw new LlmException(Kind.INVALID_RESPONSE, "Resposta da LLM não é um JSON válido", ex);
        }
    }

    private String trimDouble(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
