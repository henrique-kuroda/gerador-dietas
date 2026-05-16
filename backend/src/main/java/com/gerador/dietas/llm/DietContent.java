package com.gerador.dietas.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DietContent(
        String summary,
        int totalCalories,
        List<Meal> meals,
        Macros macros
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meal(String name, int calories, List<Item> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String food, String portion, int calories) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Macros(int proteinG, int carbsG, int fatG) {
    }
}
