package com.gerador.dietas.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerador.dietas.domain.Goal;
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
import java.util.List;
import java.util.Map;

@Service
public class DietGenerator {

    private static final Logger log = LoggerFactory.getLogger(DietGenerator.class);

    /**
     * Espelho de {@link DietContent} no formato de responseSchema do Gemini
     * (subset OpenAPI): força o JSON na decodificação, em vez de confiar só no
     * prompt. O stripCodeFences continua como cinto de segurança.
     */
    static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "required", List.of("summary", "totalCalories", "meals", "macros"),
            "properties", Map.of(
                    "summary", Map.of("type", "STRING"),
                    "totalCalories", Map.of("type", "INTEGER"),
                    "meals", Map.of(
                            "type", "ARRAY",
                            "items", Map.of(
                                    "type", "OBJECT",
                                    "required", List.of("name", "calories", "items"),
                                    "properties", Map.of(
                                            "name", Map.of("type", "STRING"),
                                            "calories", Map.of("type", "INTEGER"),
                                            "items", Map.of(
                                                    "type", "ARRAY",
                                                    "items", Map.of(
                                                            "type", "OBJECT",
                                                            "required", List.of("food", "portion", "calories"),
                                                            "properties", Map.of(
                                                                    "food", Map.of("type", "STRING"),
                                                                    "portion", Map.of("type", "STRING"),
                                                                    "calories", Map.of("type", "INTEGER"))))))),
                    "macros", Map.of(
                            "type", "OBJECT",
                            "required", List.of("proteinG", "carbsG", "fatG"),
                            "properties", Map.of(
                                    "proteinG", Map.of("type", "INTEGER"),
                                    "carbsG", Map.of("type", "INTEGER"),
                                    "fatG", Map.of("type", "INTEGER")))));

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
        DietContent content = requestAndParse(prompt);

        List<String> violations = DietContentValidator.validate(content, metabolism.targetCalories());
        if (violations.isEmpty()) {
            return new DietGeneratorResult(content, prompt);
        }

        // Uma única re-tentativa, com as violações anexadas ao prompt.
        log.warn("Plano violou regras nutricionais, re-tentando uma vez: {}", violations);
        String retryPrompt = prompt + retryInstructions(violations);
        DietContent retried = requestAndParse(retryPrompt);

        List<String> remaining = DietContentValidator.validate(retried, metabolism.targetCalories());
        if (!remaining.isEmpty()) {
            throw new LlmException(Kind.INVALID_RESPONSE,
                    "Plano fora das regras nutricionais mesmo após re-tentativa: "
                            + String.join("; ", remaining));
        }
        return new DietGeneratorResult(retried, retryPrompt);
    }

    private DietContent requestAndParse(String prompt) {
        String raw = llmService.generateJson(prompt, RESPONSE_SCHEMA);
        return parse(stripCodeFences(raw));
    }

    private static String retryInstructions(List<String> violations) {
        StringBuilder sb = new StringBuilder(
                "\n\nATENÇÃO: o plano anterior violou as regras abaixo. "
                        + "Gere um novo plano corrigindo TODOS os pontos:\n");
        for (String violation : violations) {
            sb.append("- ").append(violation).append('\n');
        }
        return sb.toString();
    }

    String buildPrompt(Profile profile, MetabolismResult metabolism) {
        String restrictions = profile.getDietaryRestrictions();
        int target = metabolism.targetCalories();
        int min = (int) Math.round(target * (1 - DietContentValidator.CALORIE_RANGE_TOLERANCE));
        int max = (int) Math.round(target * (1 + DietContentValidator.CALORIE_RANGE_TOLERANCE));

        return promptTemplate
                .replace("{sex}", profile.getSex().name())
                .replace("{age}", String.valueOf(profile.getAge()))
                .replace("{weightKg}", trimDouble(profile.getWeightKg()))
                .replace("{heightCm}", trimDouble(profile.getHeightCm()))
                .replace("{activityLevel}", profile.getActivityLevel().name())
                .replace("{goal}", profile.getGoal().name())
                .replace("{dietaryRestrictions}", (restrictions == null || restrictions.isBlank()) ? "nenhuma" : restrictions)
                .replace("{mealsPerDay}", String.valueOf(profile.getMealsPerDay()))
                .replace("{targetCalories}", String.valueOf(target))
                .replace("{targetCaloriesMin}", String.valueOf(min))
                .replace("{targetCaloriesMax}", String.valueOf(max))
                .replace("{macroGuidelines}", macroGuidelinesFor(profile));
    }

    /**
     * Faixa de macros sugerida por objetivo. Valores são guia para a LLM compor
     * o plano — a checagem hard é feita só nas calorias totais.
     */
    static String macroGuidelinesFor(Profile profile) {
        Goal goal = profile.getGoal();
        double weight = profile.getWeightKg();
        return switch (goal) {
            case AGGRESSIVE_LOSS -> String.format(
                    "- Proteína alta para preservar massa magra: %.0f a %.0f g (2.0–2.4 g/kg).%n" +
                    "- Gordura: 20–25%% das calorias.%n" +
                    "- Carboidrato: o restante das calorias.",
                    weight * 2.0, weight * 2.4);
            case LOSE_WEIGHT -> String.format(
                    "- Proteína moderada-alta para preservar massa magra: %.0f a %.0f g (1.6–2.0 g/kg).%n" +
                    "- Gordura: 25–30%% das calorias.%n" +
                    "- Carboidrato: o restante das calorias.",
                    weight * 1.6, weight * 2.0);
            case MAINTAIN -> String.format(
                    "- Proteína: %.0f a %.0f g (1.4–1.8 g/kg).%n" +
                    "- Gordura: 25–30%% das calorias.%n" +
                    "- Carboidrato: o restante das calorias.",
                    weight * 1.4, weight * 1.8);
            case GAIN_MUSCLE -> String.format(
                    "- Proteína: %.0f a %.0f g (1.6–2.0 g/kg).%n" +
                    "- Gordura: 20–30%% das calorias.%n" +
                    "- Carboidrato: priorizar para suportar treino — o restante das calorias.",
                    weight * 1.6, weight * 2.0);
            case AGGRESSIVE_GAIN -> String.format(
                    "- Proteína: %.0f a %.0f g (1.8–2.2 g/kg).%n" +
                    "- Gordura: 20–30%% das calorias.%n" +
                    "- Carboidrato: alto, para suportar superávit e treino — o restante das calorias.",
                    weight * 1.8, weight * 2.2);
        };
    }

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
