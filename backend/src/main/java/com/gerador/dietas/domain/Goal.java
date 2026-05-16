package com.gerador.dietas.domain;

public enum Goal {
    LOSE_WEIGHT(0.80),
    MAINTAIN(1.00),
    GAIN_MUSCLE(1.12);

    private final double calorieMultiplier;

    Goal(double calorieMultiplier) {
        this.calorieMultiplier = calorieMultiplier;
    }

    public double getCalorieMultiplier() {
        return calorieMultiplier;
    }
}
