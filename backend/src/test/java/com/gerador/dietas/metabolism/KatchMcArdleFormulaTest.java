package com.gerador.dietas.metabolism;

import com.gerador.dietas.domain.ActivityLevel;
import com.gerador.dietas.domain.Formula;
import com.gerador.dietas.domain.Goal;
import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.domain.Sex;
import com.gerador.dietas.support.ProfileFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class KatchMcArdleFormulaTest {

    private final KatchMcArdleFormula formula = new KatchMcArdleFormula();

    @Test
    void calcula_tmb_a_partir_da_massa_magra() {
        // Homem 80kg, 15% de gordura -> LBM = 80 × (1 - 0,15) = 68
        // TMB = 370 + 21,6 × 68 = 370 + 1468,8 = 1838,8
        Profile profile = ProfileFixtures.of(Sex.MALE, 30, 80, 180,
                ActivityLevel.MODERATE, Goal.MAINTAIN, 15.0);

        double bmr = formula.calculateBmr(profile);

        assertThat(bmr).isCloseTo(1838.8, within(0.1));
    }

    @Test
    void calcula_tmb_para_mulher_com_gordura_diferente() {
        // Mulher 60kg, 25% de gordura -> LBM = 60 × 0,75 = 45
        // TMB = 370 + 21,6 × 45 = 370 + 972 = 1342
        Profile profile = ProfileFixtures.of(Sex.FEMALE, 28, 60, 165,
                ActivityLevel.LIGHT, Goal.MAINTAIN, 25.0);

        double bmr = formula.calculateBmr(profile);

        assertThat(bmr).isCloseTo(1342.0, within(0.1));
    }

    @Test
    void falha_quando_body_fat_e_nulo() {
        Profile profile = ProfileFixtures.of(Sex.MALE, 30, 80, 180,
                ActivityLevel.MODERATE, Goal.MAINTAIN, null);

        assertThatThrownBy(() -> formula.calculateBmr(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bodyFatPercent");
    }

    @Test
    void identifica_tipo() {
        assertThat(formula.type()).isEqualTo(Formula.KATCH_MCARDLE);
    }
}
