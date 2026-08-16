package com.gerador.dietas.dto;

import com.gerador.dietas.domain.ActivityLevel;
import com.gerador.dietas.domain.DietPlan;
import com.gerador.dietas.domain.Formula;
import com.gerador.dietas.domain.Goal;
import com.gerador.dietas.domain.ProfileSnapshot;
import com.gerador.dietas.domain.Sex;

import java.time.Instant;
import java.util.Map;

public record DietPlanResponse(
        Long id,
        Integer tmb,
        Integer tdee,
        Integer targetCalories,
        Formula formulaUsed,
        Map<String, Object> content,
        Instant createdAt,
        Instant adjustedAt,
        Integer adjustmentCount,
        Integer adjustmentsRemaining,
        Integer maxAdjustments,
        ProfileSnapshotResponse profileSnapshot
) {
    public static DietPlanResponse from(DietPlan plan) {
        return new DietPlanResponse(
                plan.getId(),
                plan.getTmb(),
                plan.getTdee(),
                plan.getTargetCalories(),
                plan.getFormulaUsed(),
                plan.getContent(),
                plan.getCreatedAt(),
                plan.getAdjustedAt(),
                plan.getAdjustmentCount(),
                plan.getAdjustmentsRemaining(),
                DietPlan.MAX_ADJUSTMENTS,
                ProfileSnapshotResponse.from(plan.getProfileSnapshot())
        );
    }

    /** Nulo em planos gerados antes do snapshot existir (migration V3). */
    public record ProfileSnapshotResponse(
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
        static ProfileSnapshotResponse from(ProfileSnapshot snapshot) {
            if (snapshot == null) {
                return null;
            }
            return new ProfileSnapshotResponse(
                    snapshot.getWeightKg(),
                    snapshot.getHeightCm(),
                    snapshot.getAge(),
                    snapshot.getSex(),
                    snapshot.getActivityLevel(),
                    snapshot.getGoal(),
                    snapshot.getDietaryRestrictions(),
                    snapshot.getMealsPerDay(),
                    snapshot.getBodyFatPercent()
            );
        }
    }
}
