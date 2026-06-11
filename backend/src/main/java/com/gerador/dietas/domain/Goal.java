package com.gerador.dietas.domain;

public enum Goal {
    AGGRESSIVE_LOSS(0.70),
    LOSE_WEIGHT(0.80),
    MAINTAIN(1.00),
    GAIN_MUSCLE(1.12),
    AGGRESSIVE_GAIN(1.20);

    private final double calorieMultiplier;

    Goal(double calorieMultiplier) {
        this.calorieMultiplier = calorieMultiplier;
    }

    public double getCalorieMultiplier() {
        return calorieMultiplier;
    }
}
