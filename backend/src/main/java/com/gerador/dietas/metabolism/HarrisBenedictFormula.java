package com.gerador.dietas.metabolism;

import com.gerador.dietas.domain.Formula;
import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.domain.Sex;
import org.springframework.stereotype.Component;

/**
 * Harris-Benedict revisada por Roza & Shizgal (1984).
 */
@Component
public class HarrisBenedictFormula implements MetabolicFormula {

    @Override
    public double calculateBmr(Profile profile) {
        double weight = profile.getWeightKg();
        double height = profile.getHeightCm();
        int age = profile.getAge();

        if (profile.getSex() == Sex.MALE) {
            return 88.362 + (13.397 * weight) + (4.799 * height) - (5.677 * age);
        }
        return 447.593 + (9.247 * weight) + (3.098 * height) - (4.330 * age);
    }

    @Override
    public Formula type() {
        return Formula.HARRIS_BENEDICT;
    }
}
