package com.gerador.dietas.llm;

public interface LlmService {

    /**
     * Envia um prompt à LLM e devolve o texto bruto da resposta.
     * Espera que a resposta seja um JSON (controle de formato é feito pelo provedor).
     *
     * @throws LlmException quando há falha transitória (timeout/429/5xx), resposta vazia ou erro de config.
     */
    String generateJson(String prompt);
}
