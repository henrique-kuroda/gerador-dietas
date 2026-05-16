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

class MifflinStJeorFormulaTest {

    private final MifflinStJeorFormula formula = new MifflinStJeorFormula();

    @Test
    void calcula_tmb_para_homem_caso_referencia_spec() {
        // Homem 30 anos, 80kg, 180cm (caso de referência da seção A3):
        // (10 × 80) + (6,25 × 180) − (5 × 30) + 5
        // = 800 + 1125 − 150 + 5 = 1780
        Profile profile = ProfileFixtures.of(Sex.MALE, 30, 80, 180,
                ActivityLevel.MODERATE, Goal.LOSE_WEIGHT, null);

        double bmr = formula.calculateBmr(profile);

        assertThat(bmr).isCloseTo(1780.0, within(0.5));
    }

    @Test
    void calcula_tmb_para_mulher() {
        // Mulher 28 anos, 60kg, 165cm:
        // (10 × 60) + (6,25 × 165) − (5 × 28) − 161
        // = 600 + 1031,25 − 140 − 161 = 1330,25
        Profile profile = ProfileFixtures.of(Sex.FEMALE, 28, 60, 165,
                ActivityLevel.LIGHT, Goal.MAINTAIN, null);

        double bmr = formula.calculateBmr(profile);

        assertThat(bmr).isCloseTo(1330.25, within(0.1));
    }

    @Test
    void identifica_tipo() {
        assertThat(formula.type()).isEqualTo(Formula.MIFFLIN_ST_JEOR);
    }
}
