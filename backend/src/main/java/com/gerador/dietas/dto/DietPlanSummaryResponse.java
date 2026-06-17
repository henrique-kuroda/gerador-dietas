package com.gerador.dietas.dto;

import com.gerador.dietas.domain.DietPlan;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Resumo de um plano para a listagem do histórico — sem o {@code content}
 * completo (refeições/itens), que só é devolvido no detalhe ({@code GET
 * /api/diet/{id}}). Os campos {@code summary}, {@code totalCalories} e
 * {@code mealsCount} são extraídos do JSONB para a UI montar a lista.
 */
public record DietPlanSummaryResponse(
        Long id,
        Instant createdAt,
        Integer targetCalories,
        String summary,
        Integer totalCalories,
        int mealsCount
) {
    public static DietPlanSummaryResponse from(DietPlan plan) {
        Map<String, Object> content = plan.getContent();
        String summary = content == null ? null : asString(content.get("summary"));
        Integer totalCalories = content == null ? null : asInt(content.get("totalCalories"));
        int mealsCount = content != null && content.get("meals") instanceof List<?> meals
                ? meals.size()
                : 0;
        return new DietPlanSummaryResponse(
                plan.getId(),
                plan.getCreatedAt(),
                plan.getTargetCalories(),
                summary,
                totalCalories,
                mealsCount
        );
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer asInt(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
