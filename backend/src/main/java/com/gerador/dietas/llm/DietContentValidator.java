package com.gerador.dietas.llm;

import java.util.ArrayList;
import java.util.List;

/**
 * Checagens semânticas do plano gerado — o que separa "parseou" de "faz
 * sentido nutricional". As mensagens carregam os números violados para serem
 * anexadas ao prompt na re-tentativa.
 */
final class DietContentValidator {

    /** Mesma faixa (±5%) enviada à LLM no prompt. */
    static final double CALORIE_RANGE_TOLERANCE = 0.05;
    /** Desvio aceito entre a soma dos itens e a caloria declarada da refeição. */
    static final double MEAL_ITEMS_TOLERANCE = 0.10;
    /** Participação máxima de uma refeição no total do dia. */
    static final double MAX_MEAL_SHARE = 0.40;
    /** Com 1–2 refeições/dia a regra dos 40% é matematicamente impossível. */
    static final int MIN_MEALS_FOR_SHARE_RULE = 3;

    private DietContentValidator() {
    }

    static List<String> validate(DietContent content, int targetCalories) {
        List<String> violations = new ArrayList<>();
        List<DietContent.Meal> meals = content.meals();

        int totalFromMeals = meals.stream().mapToInt(DietContent.Meal::calories).sum();
        int min = (int) Math.round(targetCalories * (1 - CALORIE_RANGE_TOLERANCE));
        int max = (int) Math.round(targetCalories * (1 + CALORIE_RANGE_TOLERANCE));
        if (totalFromMeals < min || totalFromMeals > max) {
            violations.add("a soma das refeições foi " + totalFromMeals
                    + " kcal, fora da faixa exigida de " + min + " a " + max + " kcal");
        }

        for (DietContent.Meal meal : meals) {
            if (meal.items() == null || meal.items().isEmpty()) {
                violations.add("a refeição '" + meal.name() + "' veio sem itens");
                continue;
            }
            int itemsSum = meal.items().stream().mapToInt(DietContent.Item::calories).sum();
            if (Math.abs(itemsSum - meal.calories()) > meal.calories() * MEAL_ITEMS_TOLERANCE) {
                violations.add("a refeição '" + meal.name() + "' declara " + meal.calories()
                        + " kcal mas os itens somam " + itemsSum + " kcal (desvio acima de 10%)");
            }
        }

        if (meals.size() >= MIN_MEALS_FOR_SHARE_RULE && totalFromMeals > 0) {
            for (DietContent.Meal meal : meals) {
                if (meal.calories() > totalFromMeals * MAX_MEAL_SHARE) {
                    long share = Math.round(100.0 * meal.calories() / totalFromMeals);
                    violations.add("a refeição '" + meal.name() + "' concentra " + share
                            + "% das calorias do dia (máximo 40%)");
                }
            }
        }

        return violations;
    }
}
