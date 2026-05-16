package com.gerador.dietas.metabolism;

import com.gerador.dietas.domain.ActivityLevel;
import com.gerador.dietas.domain.Formula;
import com.gerador.dietas.domain.Goal;
import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.domain.Sex;
import com.gerador.dietas.support.ProfileFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HarrisBenedictFormulaTest {

    private final HarrisBenedictFormula formula = new HarrisBenedictFormula();

    @Test
    void calcula_tmb_para_homem() {
        // Homem 30 anos, 80kg, 180cm:
        // 88,362 + (13,397 × 80) + (4,799 × 180) − (5,677 × 30)
        // = 88,362 + 1071,76 + 863,82 − 170,31 = 1853,632
        Profile profile = ProfileFixtures.of(Sex.MALE, 30, 80, 180,
                ActivityLevel.MODERATE, Goal.MAINTAIN, null);

        double bmr = formula.calculateBmr(profile);

        assertThat(bmr).isCloseTo(1853.63, within(0.1));
    }

    @Test
    void calcula_tmb_para_mulher() {
        // Mulher 28 anos, 60kg, 165cm:
        // 447,593 + (9,247 × 60) + (3,098 × 165) − (4,330 × 28)
        // = 447,593 + 554,82 + 511,17 − 121,24 = 1392,343
        Profile profile = ProfileFixtures.of(Sex.FEMALE, 28, 60, 165,
                ActivityLevel.LIGHT, Goal.MAINTAIN, null);

        double bmr = formula.calculateBmr(profile);

        assertThat(bmr).isCloseTo(1392.34, within(0.1));
    }

    @Test
    void identifica_tipo() {
        assertThat(formula.type()).isEqualTo(Formula.HARRIS_BENEDICT);
    }
}
