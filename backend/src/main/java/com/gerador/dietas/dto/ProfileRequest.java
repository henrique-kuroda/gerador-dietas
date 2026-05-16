package com.gerador.dietas.dto;

import com.gerador.dietas.domain.ActivityLevel;
import com.gerador.dietas.domain.Goal;
import com.gerador.dietas.domain.Sex;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProfileRequest(

        @NotNull(message = "é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "deve ser maior que zero")
        @DecimalMax(value = "500.0", message = "valor irreal")
        Double weightKg,

        @NotNull(message = "é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "deve ser maior que zero")
        @DecimalMax(value = "300.0", message = "valor irreal")
        Double heightCm,

        @NotNull(message = "é obrigatório")
        @Min(value = 1, message = "deve ser maior que zero")
        @Max(value = 120, message = "deve ser no máximo 120")
        Integer age,

        @NotNull(message = "é obrigatório")
        Sex sex,

        @NotNull(message = "é obrigatório")
        ActivityLevel activityLevel,

        @NotNull(message = "é obrigatório")
        Goal goal,

        @Size(max = 1000, message = "deve ter no máximo 1000 caracteres")
        String dietaryRestrictions,

        @NotNull(message = "é obrigatório")
        @Min(value = 1, message = "deve ser pelo menos 1")
        @Max(value = 8, message = "deve ser no máximo 8")
        Integer mealsPerDay,

        @DecimalMin(value = "0.0", message = "deve ser maior ou igual a zero")
        @DecimalMax(value = "100.0", inclusive = false, message = "deve ser menor que 100")
        Double bodyFatPercent
) {
}
