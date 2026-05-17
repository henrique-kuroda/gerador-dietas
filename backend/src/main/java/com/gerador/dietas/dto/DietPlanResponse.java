package com.gerador.dietas.dto;

import com.gerador.dietas.domain.DietPlan;
import com.gerador.dietas.domain.Formula;

import java.time.Instant;
import java.util.Map;

public record DietPlanResponse(
        Long id,
        Integer tmb,
        Integer tdee,
        Integer targetCalories,
        Formula formulaUsed,
        Map<String, Object> content,
        Instant createdAt
) {
    public static DietPlanResponse from(DietPlan plan) {
        return new DietPlanResponse(
                plan.getId(),
                plan.getTmb(),
                plan.getTdee(),
                plan.getTargetCalories(),
                plan.getFormulaUsed(),
                plan.getContent(),
                plan.getCreatedAt()
        );
    }
}
