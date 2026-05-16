package com.gerador.dietas.metabolism;

import com.gerador.dietas.domain.Formula;
import com.gerador.dietas.domain.Profile;

public interface MetabolicFormula {

    double calculateBmr(Profile profile);

    Formula type();
}
