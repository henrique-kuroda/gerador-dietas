package com.gerador.dietas.metabolism;

import com.gerador.dietas.domain.Formula;
import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.domain.Sex;
import org.springframework.stereotype.Component;

/**
 * Mifflin-St Jeor — padrão quando não há percentual de gordura corporal informado.
 */
@Component
public class MifflinStJeorFormula implements MetabolicFormula {

    @Override
    public double calculateBmr(Profile profile) {
        double weight = profile.getWeightKg();
        double height = profile.getHeightCm();
        int age = profile.getAge();
        double base = (10 * weight) + (6.25 * height) - (5 * age);

        return profile.getSex() == Sex.MALE ? base + 5 : base - 161;
    }

    @Override
    public Formula type() {
        return Formula.MIFFLIN_ST_JEOR;
    }
}
