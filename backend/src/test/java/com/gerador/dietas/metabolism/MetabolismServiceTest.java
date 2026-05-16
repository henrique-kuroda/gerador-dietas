package com.gerador.dietas.metabolism;

import com.gerador.dietas.domain.ActivityLevel;
import com.gerador.dietas.domain.Formula;
import com.gerador.dietas.domain.Goal;
import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.domain.Sex;
import com.gerador.dietas.support.ProfileFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetabolismServiceTest {

    private final MetabolismService service = new MetabolismService(List.of(
            new HarrisBenedictFormula(),
            new MifflinStJeorFormula(),
            new KatchMcArdleFormula()
    ));

    @Test
    void caso_referencia_seca_homem_30_80kg_180cm_moderate_lose_weight() {
        // Spec A3 — caso de referência:
        // Mifflin: TMB ≈ 1780  ->  TDEE ≈ 1780 × 1.55 = 2759  ->  alvo ≈ 2759 × 0.80 ≈ 2207
        Profile profile = ProfileFixtures.of(Sex.MALE, 30, 80, 180,
                ActivityLevel.MODERATE, Goal.LOSE_WEIGHT, null);

        MetabolismResult result = service.calculate(profile);

        assertThat(result.formulaUsed()).isEqualTo(Formula.MIFFLIN_ST_JEOR);
        assertThat(result.tmb()).isEqualTo(1780);
        assertThat(result.tdee()).isEqualTo(2759);
        assertThat(result.targetCalories()).isEqualTo(2207);
    }

    @Test
    void seleciona_katch_mcardle_quando_body_fat_informado() {
        Profile profile = ProfileFixtures.of(Sex.MALE, 30, 80, 180,
                ActivityLevel.MODERATE, Goal.MAINTAIN, 15.0);

        MetabolismResult result = service.calculate(profile);

        assertThat(result.formulaUsed()).isEqualTo(Formula.KATCH_MCARDLE);
        // TMB = 1838,8 -> TDEE = 1838,8 × 1.55 = 2850,14 -> alvo = TDEE × 1.0 = 2850
        assertThat(result.tmb()).isEqualTo(1839);
        assertThat(result.tdee()).isEqualTo(2850);
        assertThat(result.targetCalories()).isEqualTo(2850);
    }

    @Test
    void seleciona_mifflin_quando_body_fat_ausente() {
        Profile profile = ProfileFixtures.of(Sex.FEMALE, 28, 60, 165,
                ActivityLevel.LIGHT, Goal.MAINTAIN, null);

        MetabolismResult result = service.calculate(profile);

        assertThat(result.formulaUsed()).isEqualTo(Formula.MIFFLIN_ST_JEOR);
    }

    @Test
    void aplica_multiplicador_de_superavit_para_ganho_de_massa() {
        Profile profile = ProfileFixtures.of(Sex.MALE, 30, 80, 180,
                ActivityLevel.MODERATE, Goal.GAIN_MUSCLE, null);

        MetabolismResult result = service.calculate(profile);

        // TMB=1780, TDEE=2759, alvo = 2759 × 1.12 = 3090,08
        assertThat(result.targetCalories()).isEqualTo(3090);
    }

    @Test
    void aplica_fator_de_atividade_sedentary() {
        Profile profile = ProfileFixtures.of(Sex.MALE, 30, 80, 180,
                ActivityLevel.SEDENTARY, Goal.MAINTAIN, null);

        MetabolismResult result = service.calculate(profile);

        // TMB=1780, TDEE = 1780 × 1.2 = 2136
        assertThat(result.tdee()).isEqualTo(2136);
        assertThat(result.targetCalories()).isEqualTo(2136);
    }

    @Test
    void seleciona_diretamente_via_select_formula() {
        Profile withBf = ProfileFixtures.of(Sex.MALE, 30, 80, 180,
                ActivityLevel.MODERATE, Goal.MAINTAIN, 20.0);
        Profile withoutBf = ProfileFixtures.of(Sex.MALE, 30, 80, 180,
                ActivityLevel.MODERATE, Goal.MAINTAIN, null);

        assertThat(service.selectFormula(withBf)).isEqualTo(Formula.KATCH_MCARDLE);
        assertThat(service.selectFormula(withoutBf)).isEqualTo(Formula.MIFFLIN_ST_JEOR);
    }
}
