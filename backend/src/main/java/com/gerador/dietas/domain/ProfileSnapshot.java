package com.gerador.dietas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Cópia imutável do perfil no momento da geração da dieta. Mantém o histórico
 * coerente: se o usuário atualizar o perfil depois, PDF e detalhe continuam
 * exibindo os dados que originaram os cálculos.
 */
@Embeddable
public class ProfileSnapshot {

    @Column(name = "profile_weight_kg")
    private Double weightKg;

    @Column(name = "profile_height_cm")
    private Double heightCm;

    @Column(name = "profile_age")
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_sex", length = 20)
    private Sex sex;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_activity_level", length = 20)
    private ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_goal", length = 20)
    private Goal goal;

    @Column(name = "profile_dietary_restrictions", length = 1000)
    private String dietaryRestrictions;

    @Column(name = "profile_meals_per_day")
    private Integer mealsPerDay;

    @Column(name = "profile_body_fat_percent")
    private Double bodyFatPercent;

    protected ProfileSnapshot() {
    }

    public static ProfileSnapshot from(Profile profile) {
        ProfileSnapshot snapshot = new ProfileSnapshot();
        snapshot.weightKg = profile.getWeightKg();
        snapshot.heightCm = profile.getHeightCm();
        snapshot.age = profile.getAge();
        snapshot.sex = profile.getSex();
        snapshot.activityLevel = profile.getActivityLevel();
        snapshot.goal = profile.getGoal();
        snapshot.dietaryRestrictions = profile.getDietaryRestrictions();
        snapshot.mealsPerDay = profile.getMealsPerDay();
        snapshot.bodyFatPercent = profile.getBodyFatPercent();
        return snapshot;
    }

    /** Planos anteriores à V3 não têm snapshot — todas as colunas vêm nulas. */
    public boolean isEmpty() {
        return weightKg == null && heightCm == null && age == null && sex == null;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public Double getHeightCm() {
        return heightCm;
    }

    public Integer getAge() {
        return age;
    }

    public Sex getSex() {
        return sex;
    }

    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public Goal getGoal() {
        return goal;
    }

    public String getDietaryRestrictions() {
        return dietaryRestrictions;
    }

    public Integer getMealsPerDay() {
        return mealsPerDay;
    }

    public Double getBodyFatPercent() {
        return bodyFatPercent;
    }
}
