package com.gerador.dietas.domain;

/** Tipo de chamada paga à LLM registrada em {@code llm_usage}. */
public enum LlmCallKind {
    GENERATE,
    ADJUST
}
