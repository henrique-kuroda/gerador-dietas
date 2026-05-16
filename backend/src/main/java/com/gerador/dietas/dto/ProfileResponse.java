package com.gerador.dietas.dto;

import com.gerador.dietas.domain.ActivityLevel;
import com.gerador.dietas.domain.Goal;
import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.domain.Sex;

public record ProfileResponse(
        Double weightKg,
        Double heightCm,
        Integer age,
        Sex sex,
        ActivityLevel activityLevel,
        Goal goal,
        String dietaryRestrictions,
        Integer mealsPerDay,
        Double bodyFatPercent
) {
    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
                profile.getWeightKg(),
                profile.getHeightCm(),
                profile.getAge(),
                profile.getSex(),
                profile.getActivityLevel(),
                profile.getGoal(),
                profile.getDietaryRestrictions(),
                profile.getMealsPerDay(),
                profile.getBodyFatPercent()
        );
    }
}
