package com.gerador.dietas.support;

import com.gerador.dietas.domain.ActivityLevel;
import com.gerador.dietas.domain.Goal;
import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.domain.Sex;

public final class ProfileFixtures {

    private ProfileFixtures() {
    }

    public static Profile of(Sex sex, int age, double weightKg, double heightCm,
                             ActivityLevel activityLevel, Goal goal, Double bodyFatPercent) {
        Profile p = new Profile();
        p.setSex(sex);
        p.setAge(age);
        p.setWeightKg(weightKg);
        p.setHeightCm(heightCm);
        p.setActivityLevel(activityLevel);
        p.setGoal(goal);
        p.setMealsPerDay(4);
        p.setBodyFatPercent(bodyFatPercent);
        return p;
    }

    public static Profile of(Sex sex, int age, double weightKg, double heightCm,
                             ActivityLevel activityLevel, Goal goal,
                             int mealsPerDay, String dietaryRestrictions, Double bodyFatPercent) {
        Profile p = of(sex, age, weightKg, heightCm, activityLevel, goal, bodyFatPercent);
        p.setMealsPerDay(mealsPerDay);
        p.setDietaryRestrictions(dietaryRestrictions);
        return p;
    }
}
