package com.gerador.dietas.exception;

public class DietPlanNotFoundException extends RuntimeException {
    public DietPlanNotFoundException(String message) {
        super(message);
    }
}
