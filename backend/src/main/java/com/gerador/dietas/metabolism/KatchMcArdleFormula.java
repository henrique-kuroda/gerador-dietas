package com.gerador.dietas.metabolism;

import com.gerador.dietas.domain.Formula;
import com.gerador.dietas.domain.Profile;
import org.springframework.stereotype.Component;

/**
 * Katch-McArdle — usar apenas quando o percentual de gordura corporal for conhecido.
 */
@Component
public class KatchMcArdleFormula implements MetabolicFormula {

    @Override
    public double calculateBmr(Profile profile) {
        Double bodyFatPercent = profile.getBodyFatPercent();
        if (bodyFatPercent == null) {
            throw new IllegalArgumentException(
                    "Katch-McArdle requer bodyFatPercent; use outra fórmula quando indisponível.");
        }
        double leanBodyMass = profile.getWeightKg() * (1 - bodyFatPercent / 100.0);
        return 370 + (21.6 * leanBodyMass);
    }

    @Override
    public Formula type() {
        return Formula.KATCH_MCARDLE;
    }
}
