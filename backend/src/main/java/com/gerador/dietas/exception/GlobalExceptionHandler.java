package com.gerador.dietas.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.gerador.dietas.llm.LlmException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ApiError> handleProfileNotFound(ProfileNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(ProfileIncompleteException.class)
    public ResponseEntity<ApiError> handleProfileIncomplete(ProfileIncompleteException ex) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(DietPlanNotFoundException.class)
    public ResponseEntity<ApiError> handleDietPlanNotFound(DietPlanNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(DietGenerationLimitException.class)
    public ResponseEntity<ApiError> handleDietGenerationLimit(DietGenerationLimitException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLocking(OptimisticLockingFailureException ex) {
        // Duas alterações concorrentes no mesmo plano (ex.: dois ajustes em paralelo).
        // A segunda perde: o cliente recarrega e decide se ainda quer o ajuste.
        return build(HttpStatus.CONFLICT, "Conflict",
                "Este plano foi alterado por outra requisição. Recarregue a dieta e tente novamente.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        String message = "Corpo da requisição inválido";
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            String field = ife.getPath().isEmpty() ? "campo" : ife.getPath().get(0).getFieldName();
            message = field + " com valor inválido: " + ife.getValue();
        }
        return build(HttpStatus.BAD_REQUEST, "Bad Request", message);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Credenciais inválidas");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Autenticação requerida");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Validação falhou";
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<ApiError> handleLlm(LlmException ex) {
        return switch (ex.getKind()) {
            case UNAVAILABLE, RATE_LIMITED ->
                    build(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                            "Serviço de IA temporariamente indisponível. Tente novamente em instantes.");
            case INVALID_RESPONSE ->
                    build(HttpStatus.BAD_GATEWAY, "Bad Gateway",
                            "Resposta inválida da IA. Tente gerar a dieta novamente.");
            case CONFIGURATION ->
                    build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                            "Erro de configuração do serviço de IA.");
        };
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + " " + (fe.getDefaultMessage() == null ? "é inválido" : fe.getDefaultMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), error, message));
    }
}
