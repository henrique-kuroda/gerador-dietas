package com.gerador.dietas.llm;

public class LlmException extends RuntimeException {

    public enum Kind {
        /** Timeout, indisponibilidade ou 5xx — operação transitória, vale tentar novamente. */
        UNAVAILABLE,
        /** Rate limit atingido (429). */
        RATE_LIMITED,
        /** Resposta da LLM veio vazia ou em formato não-parseável. */
        INVALID_RESPONSE,
        /** Erro de configuração (sem chave de API, etc.). */
        CONFIGURATION
    }

    private final Kind kind;

    public LlmException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public LlmException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
