package com.gerador.dietas.metabolism;

import com.gerador.dietas.domain.Formula;

public record MetabolismResult(
        int tmb,
        int tdee,
        int targetCalories,
        Formula formulaUsed
) {
}
