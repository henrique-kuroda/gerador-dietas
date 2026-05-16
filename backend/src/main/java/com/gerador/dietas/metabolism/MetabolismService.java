package com.gerador.dietas.metabolism;

import com.gerador.dietas.domain.Formula;
import com.gerador.dietas.domain.Profile;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class MetabolismService {

    private final Map<Formula, MetabolicFormula> formulas;

    public MetabolismService(List<MetabolicFormula> formulas) {
        Map<Formula, MetabolicFormula> map = new EnumMap<>(Formula.class);
        for (MetabolicFormula f : formulas) {
            map.put(f.type(), f);
        }
        this.formulas = map;
    }

    public MetabolismResult calculate(Profile profile) {
        Formula chosen = selectFormula(profile);
        MetabolicFormula formula = formulas.get(chosen);
        if (formula == null) {
            throw new IllegalStateException("Fórmula não registrada: " + chosen);
        }

        double bmr = formula.calculateBmr(profile);
        double tdee = bmr * profile.getActivityLevel().getFactor();
        double target = tdee * profile.getGoal().getCalorieMultiplier();

        return new MetabolismResult(
                (int) Math.round(bmr),
                (int) Math.round(tdee),
                (int) Math.round(target),
                chosen
        );
    }

    /**
     * Regra de seleção:
     * - Katch-McArdle se houver bodyFatPercent informado (usa massa magra, mais preciso).
     * - Caso contrário, Mifflin-St Jeor (padrão recomendado quando não há % de gordura).
     * Harris-Benedict fica disponível, mas não é selecionado automaticamente.
     */
    Formula selectFormula(Profile profile) {
        if (profile.getBodyFatPercent() != null) {
            return Formula.KATCH_MCARDLE;
        }
        return Formula.MIFFLIN_ST_JEOR;
    }
}
