package com.gerador.dietas.llm;

import java.util.Map;

public interface LlmService {

    /**
     * Envia um prompt à LLM e devolve o texto bruto da resposta.
     * Espera que a resposta seja um JSON (controle de formato é feito pelo provedor).
     *
     * @throws LlmException quando há falha transitória (timeout/429/5xx), resposta vazia ou erro de config.
     */
    String generateJson(String prompt);

    /**
     * Variante com schema de resposta (subset OpenAPI, formato do provedor) que
     * força o formato do JSON na decodificação. Provedores sem suporte ignoram
     * o schema e caem na variante por prompt.
     */
    default String generateJson(String prompt, Map<String, Object> responseSchema) {
        return generateJson(prompt);
    }
}
